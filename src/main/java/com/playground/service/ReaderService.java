package com.playground.service;

import com.playground.dataobject.StockObject;
import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

import java.io.IOException;
import java.math.BigDecimal;
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
@AllArgsConstructor
@Service
public class ReaderService {


    /**
     * Stock getter stock object.
     *
     * @param companySymbol the company symbol
     * @return the stock object
     */
    public StockObject stockGetter(final String companySymbol) {
        try {
            return new StockObject(YahooFinance.get(companySymbol));
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * Stock getter on date stock object.
     *
     * @param companySymbol the company symbol
     * @param dateString    the date string
     * @return the stock object
     */
    public StockObject stockGetterOnDate(final String companySymbol, String dateString) {

        try {
            // TO DO - refactor this exception part
            SimpleDateFormat dataFormat = new SimpleDateFormat("dd-MM-yyyy");
            Date date = dataFormat.parse(dateString);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            return new StockObject(YahooFinance.get(companySymbol, cal, Interval.DAILY));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * data intialisation for datatbases
     * used for testing only
     *
     * @param jdbcTemplate database beans reference
     * @param iterator     Iterator over Yahoo API historical quotes
     */
    private synchronized void initData(JdbcTemplate jdbcTemplate, Iterator<HistoricalQuote> iterator) {
        ArrayList<Date> dataArray = new ArrayList<>();
        String reserveCompanyName = null;
        while (iterator.hasNext()) {
            HistoricalQuote tempObject = iterator.next();
            String company = tempObject.getSymbol();
            reserveCompanyName = company;
            BigDecimal value = tempObject.getClose();
            Calendar date = tempObject.getDate();
            dataArray.add(date.getTime());
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String dateStr = dateFormat.format(date.getTime());
            if (dateStr == null) {
                dateStr = LocalDate.now().toString();
            }

            jdbcTemplate.execute("INSERT OR IGNORE INTO companiesCatalogue  (companyName) VALUES ('" + company + "')");
            jdbcTemplate.execute("INSERT OR IGNORE INTO company  (companyID ,value,date) VALUES ((SELECT id FROM companiesCatalogue WHERE companyName ='" + company + "'),'" + value + "','" + dateStr + "')");
            //to to try to add only once - right now done via database constraint
            jdbcTemplate.execute("INSERT OR IGNORE INTO portfolio  (companyID ,numStocks) VALUES ((SELECT id FROM companiesCatalogue WHERE companyName ='" + company + "'),'" + getRandomNumber(0, 50000) + "')");
        }
        int numberOfTransactions = getRandomNumber(0, 10);
        Collections.sort(dataArray);

        for (int i = 1; i <= numberOfTransactions; i++) {
            Date date = generateRandomDate(dataArray.get(0), dataArray.get(dataArray.size() - 1));
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String dateStr = dateFormat.format(date);
            int enumChooser = getRandomNumber(0, 2);
            jdbcTemplate.execute("INSERT INTO transactions  (company , [transaction],date) VALUES ((SELECT id FROM companiesCatalogue WHERE companyName ='" + reserveCompanyName + "'),'" + ((enumChooser > 0) ? "BUY" : "SELL") + "','" + dateStr + "')");
        }
    }

    /**
     * Decode stock.
     *
     * @param thisStock    the this stock
     * @param jdbcTemplate the jdbc template
     * @param initData     the init data
     * @throws IOException the io exception
     */
    public void decodeStock(final StockObject thisStock, JdbcTemplate jdbcTemplate, boolean initData) throws IOException {
        Iterator<HistoricalQuote> iterator = thisStock.getActualStock().getHistory().iterator();
        if (initData) {
            initData(jdbcTemplate, iterator);
        }
    }

    /**
     * Serve request list.
     *
     * @param object       the object
     * @param jdbcTemplate the jdbc template
     * @return the list
     */
    public List<StockObject.DatabaseObject> serveRequest(StockObject object, JdbcTemplate jdbcTemplate) {
        return object.getCompanyValues(jdbcTemplate);
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

}
