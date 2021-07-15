package com.playground.service;

import com.playground.dataobject.StockObject;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The type Reader service.
 * Reads Yahoo values
 * Creates Stock objects
 * Updates databases
 */
@Getter
@Setter
@Component
public class ReaderService {
    @Resource
    //list of stock objects read from property file
    private List<StockObject> stockObjectList;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String calculateAtDate;

    private String dateFormat;

    // data frequency extraction : e.q. DAILY / MONTHLY )
    private Interval frequencyFrame;
    //file to be used for portfolio print out - see configuration file
    private String outputFileName;


    //TODO add input data validation
    //dubbuging only
    void setUp() {
        System.out.println("Preparing test");

        //Create the database tables:

        jdbcTemplate.execute(" CREATE TABLE IF NOT EXISTS companiesCatalogue (\n" +
                "                id          INTEGER PRIMARY KEY,\n" +
                "                companyName STRING  UNIQUE\n" +
                "                NOT NULL\n" +
                "        );");


        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS company (\n" +
                "    id        INTEGER PRIMARY KEY ASC AUTOINCREMENT\n" +
                "                      NOT NULL,\n" +
                "    companyID INTEGER NOT NULL\n" +
                "                      REFERENCES companiesCatalogue (id),\n" +
                "    date      DATE,\n" +
                "    value     DOUBLE  NOT NULL\n" +
                ");");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS portfolio (\n" +
                "    id               INTEGER PRIMARY KEY\n" +
                "                             NOT NULL,\n" +
                "    numStocksCurrent BIGINT  DEFAULT (0),\n" +
                "    companyID        INTEGER NOT NULL\n" +
                "                             REFERENCES companiesCatalogue (id) \n" +
                "                             UNIQUE ON CONFLICT IGNORE,\n" +
                "    numStocksStart   BIGINT  DEFAULT (0) \n" +
                ");");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS transactions (\n" +
                "    id        INTEGER  PRIMARY KEY AUTOINCREMENT,\n" +
                "    companyID INTEGER  REFERENCES company (id) \n" +
                "                       NOT NULL,\n" +
                "    quantity  BIGINT   DEFAULT (0),\n" +
                "    transact  STRING   NOT NULL,\n" +
                "    date      DATETIME NOT NULL\n" +
                ");");

    }

    /**
     * Tear down.
     * used to clean database
     * later on this needs to be refactored
     */
    public void tearDown() {
        System.out.println("Finishing test");
        jdbcTemplate.execute("delete from companiesCatalogue;");
        jdbcTemplate.execute("delete from  company;");
        jdbcTemplate.execute("delete from  portfolio;");
        jdbcTemplate.execute("delete from transactions;");
    }

    /**
     * data intialisation for datatbases just for the fun of the esercise for now
     * <p>
     * to be updated with configuration data reader
     *
     * @param iterator Iterator over Yahoo API historical quotes
     */
    private synchronized void initData(Iterator<HistoricalQuote> iterator) {
        ArrayList<Date> dataArray = new ArrayList<>();
        String reserveCompanyName = null;
        while (iterator.hasNext()) {
            HistoricalQuote tempObject = iterator.next();
            String company = tempObject.getSymbol();
            reserveCompanyName = company;
            BigDecimal value = tempObject.getClose();
            Calendar date = tempObject.getDate();
            dataArray.add(date.getTime());
            SimpleDateFormat dateFormat = new SimpleDateFormat(this.dateFormat);
            String dateStr = dateFormat.format(date.getTime());
            if (dateStr == null) {
                dateStr = LocalDate.now().toString();
            }

            jdbcTemplate.execute("INSERT OR IGNORE INTO companiesCatalogue  (companyName) VALUES ('" + company + "')");
            jdbcTemplate.execute("INSERT OR IGNORE INTO company  (companyID ,value,date) VALUES ((SELECT id FROM companiesCatalogue WHERE companyName ='" + company + "'),'" + value + "','" + dateStr + "')");
            //to to try to add only once - right now done via database constraint
            jdbcTemplate.execute("INSERT OR IGNORE INTO portfolio  (companyID ,numStocksStart) VALUES ((SELECT id FROM companiesCatalogue WHERE companyName ='" + company + "'),'" + getRandomNumber(0, 50000) + "')");
        }
        int numberOfTransactions = getRandomNumber(0, 10);
        Collections.sort(dataArray);

        for (int i = 1; i <= numberOfTransactions; i++) {
            Date date = generateRandomDate(dataArray.get(0), dataArray.get(dataArray.size() - 1));
            SimpleDateFormat dateFormat = new SimpleDateFormat(this.dateFormat);
            String dateStr = dateFormat.format(date);
            int enumChooser = getRandomNumber(0, 2);
            long quantity = getRandomNumber(1, 10);
            jdbcTemplate.execute("INSERT INTO transactions  (companyID , transact,date,quantity) VALUES ((SELECT id FROM companiesCatalogue WHERE companyName ='" + reserveCompanyName + "'),'" + ((enumChooser > 0) ? "BUY" : "SELL") + "','" + dateStr + "','" + quantity + "');");
        }
    }

    /**
     * Decode stockObject.
     * calls service to populate stockObject with Yahoo informations
     * populates database tables of transactions and portfolio with ramdom data if initData = true
     *
     * @param initData the init data
     * @throws IOException the io exception
     */
    public void decodeStock(boolean initData) throws IOException {

        for (StockObject stockObject : stockObjectList) {
            try {
                // TO DO - refactor this exception part
                SimpleDateFormat dataFormat;
                dataFormat = new SimpleDateFormat(this.dateFormat);
                Date date = dataFormat.parse(stockObject.getDateString());
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                stockObject.setStock(YahooFinance.get(stockObject.getCompanySymbol(), cal, frequencyFrame));
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            if (initData) initData(stockObject.getStock().getHistory().iterator());
        }
    }

    /**
     * Serve request list.
     * for each stock objectin the stockObjectList calculates portfolio value and writes portfolio to file based on xml configuration
     */
    public void serveRequest() {
        boolean needToAppend = false;
        for (int i = 0; i < stockObjectList.size(); i++) {
            if (i > 0) {
                needToAppend = true;
            }
            toFile(stockObjectList.get(i).getCompanyValues(this.jdbcTemplate, returnDateFromString(this.calculateAtDate)), needToAppend);
        }
    }


    /**
     * converts current value to json
     *
     * @param jsonObject the json object
     * @param append     parameter to specifies wheter to append or create new file
     */
    public void toFile(JSONObject jsonObject, boolean append) {
        byte[] buff;
        String jsonStr = jsonObject.toString();

        FileOutputStream out = null;
        File file = new File(outputFileName);

        // Check if the directory exists, create the directory if it does not exist
        if (!file.getParentFile().exists()) {
            boolean mkdirs = file.getParentFile().mkdirs();
            System.out.println("File creation called and result was " + mkdirs);
        }
        try {
            buff = jsonStr.getBytes();
            out = new FileOutputStream(file, append);
            System.out.println("Output file directory:" + outputFileName);
            out.write(buff, 0, buff.length);
            System.out.println("Output json data to file successfully : " + jsonStr);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * Generated random integer between 2 numbers
     *
     * @param min min
     * @param max max
     * @return int
     */
    private int getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }

    /**
     * Generate random date date.
     *
     * @param start the start
     * @param other the other
     * @return the date
     */
    public Date generateRandomDate(Date start, Date other) {
        long startMillis = start.getTime();
        long endMillis = other.getTime();
        long randomMillisSinceEpoch = ThreadLocalRandom
                .current()
                .nextLong(startMillis, endMillis);
        return new Date(randomMillisSinceEpoch);
    }

    private Date returnDateFromString(String date) {
        DateFormat df = new SimpleDateFormat(this.dateFormat);
        try {
            return new java.sql.Date(df.parse(date).getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }
}
