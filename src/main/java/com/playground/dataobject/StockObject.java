package com.playground.dataobject;


import lombok.Getter;
import lombok.Setter;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import yahoofinance.Stock;
import yahoofinance.histquotes.HistoricalQuote;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

/**
 * The type Stock object.
 */
@Getter
@Setter

public class StockObject {

    private Stock stock;
    private String companySymbol;
    private String dateString;
    private LocalDateTime dateAccessed;
    private HistoricalQuote historicalQuote;

    private JdbcTemplate jdbcTemplate;

    private String outputFileName;

    StockObject() throws ParseException {
        this.dateAccessed = LocalDateTime.now();
    }


    /**
     * Gets all company values.
     *
     * @param jdbcTemplate the jdbc template
     * @return the company values
     */
    public List<DatabaseObject> getCompanyValues(JdbcTemplate jdbcTemplate, Date date) {
        String sql = "SELECT * FROM companiesCatalogue where companyName='" + this.getCompanySymbol() + "'";
        this.jdbcTemplate = jdbcTemplate;
        List<DatabaseObject> tempObject = jdbcTemplate.query(sql, new DatabaseObjectMapper());
        BigDecimal initialStock = tempObject.get(0).getPorfolio().get(0).getNumStocksStart();
        List<TransactionsObject> transactionsObjectList = tempObject.get(0).getTransactions();
        ListIterator<TransactionsObject> litr;
        litr = transactionsObjectList.listIterator();
        BigDecimal value = BigDecimal.valueOf(0);
        while (litr.hasNext()) {
            TransactionsObject portfolioList = litr.next();
            System.out.println(portfolioList.toString());
            if (portfolioList.getDate().compareTo(date) < 1) {
                switch (portfolioList.transact) {
                    case "BUY":
                        value = value.add(portfolioList.getQuantity());
                        break;
                    case "SELL":
                        value = value.subtract(portfolioList.getQuantity());
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + portfolioList.transact);
                }
            }
            //  System.out.println("objectDate:" + a.getDate() + " transactionType:" + a.transact + "quantity:" + a.getQuantity());
            //System.out.println(value);
        }
        if (initialStock.compareTo(value) >= 0) {
            tempObject.get(0).getPorfolio().get(0).setNumStocksCurrent(initialStock.add(value));
        }
        //System.out.println(tempObject.get(0).getPorfolio().get(0));

        BigDecimal numStocksAtDate = tempObject.get(0).getPorfolio().get(0).getNumStocksCurrent();
        BigDecimal stockValueAtDate = BigDecimal.ZERO;
        List<CompanyValue> cl = tempObject.get(0).getList();

        for (ListIterator<CompanyValue> cliterator = cl.listIterator(); cliterator.hasNext(); ) {
            Date dateOld = cliterator.next().getDate();
            if (dateOld.compareTo(date) < 1) {
                stockValueAtDate = cliterator.previous().getValue();
                break;
            }
        }

        tempObject.get(0).setValueAtDate(numStocksAtDate.multiply(stockValueAtDate));
        System.out.println("Value at requested date:" + tempObject.get(0).getValueAtDate().toString());
        System.out.println(tempObject.get(0).toString());
        //add values to JSON file
        org.json.JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(this.getCompanySymbol(), tempObject.get(0).getValueAtDate());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.toJson(jsonObject);
        return tempObject;
    }

    /**
     * converts current value to json
     *
     * @param jsonObject the json object
     */
    public void toJson(JSONObject jsonObject) {
        byte[] buff = new byte[]{};
        String jsonStr = jsonObject.toString();

        FileOutputStream out = null;
        File file = new File(outputFileName);
        // Check if the directory exists, create the directory if it does not exist
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try {
            buff = jsonStr.getBytes();
            //out=new FileOutputStream(outputFileName);
            out = new FileOutputStream(file);
            System.out.println("Output file directory:" + outputFileName);
            out.write(buff, 0, buff.length);
            System.out.println("Output json data to file successfully");
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

    private enum Transact {
        SELL,
        BUY
    }

    /**
     * The  Transactions table object type
     */
    @Getter
    @Setter
    private class TransactionsObject {
        private Long Id;
        private BigDecimal quantity;
        private Long companyID;
        private String transact;
        private Date date;

        @Override
        public String toString() {
            return "TransactionsObject{" +
                    "Id=" + Id +
                    ", quantity=" + quantity +
                    ", companyID=" + companyID +
                    ", transact='" + transact + '\'' +
                    ", date=" + date +
                    '}';
        }
    }

    /**
     * The type Transactions value mapper.
     */
    public class TransactionsValueMapper implements RowMapper<TransactionsObject> {
        @Override
        public TransactionsObject mapRow(ResultSet rs, int rowNum) throws SQLException {
            TransactionsObject data = new TransactionsObject();
            data.setId(rs.getLong("id"));
            data.setCompanyID(rs.getLong("companyID"));
            data.setQuantity(rs.getBigDecimal("quantity"));
            data.setTransact(rs.getString("transact"));
            try {
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                Date tempDate = new java.sql.Date(df.parse(rs.getString("date")).getTime());
                data.setDate(tempDate);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            return data;
        }
    }

    /**
     * The  Company table object type
     */
    @Getter
    @Setter
    private class CompanyValue {
        private Long id;
        private Long companyID;
        private Date date;
        private BigDecimal value;

        @Override
        public String toString() {
            return "CompanyValue{" +
                    "id=" + id +
                    ", companyID=" + companyID +
                    ", date=" + date +
                    ", value=" + value +
                    '}';
        }
    }

    @Getter
    @Setter
    public class DatabaseObject {
        private Long Id;
        private String companyName;
        private List<CompanyValue> list;
        private List<PorfolioObject> porfolio;
        private List<TransactionsObject> transactions;
        private BigDecimal valueAtDate = BigDecimal.ZERO;

        public void toString(Date date) {

        }

        @Override
        public String toString() {
            return "DatabaseObject{" +
                    "Id=" + Id +
                    ", companyName='" + companyName + '\'' +
                    ", list=" + list.toString() +
                    ", porfolio=" + porfolio.toString() +
                    ", transactions=" + transactions.toString() +
                    '}';
        }
    }

    /**
     * The  Porfolio table object type
     */
    @Getter
    @Setter
    private class PorfolioObject {
        private Long Id;
        private BigDecimal numStocksCurrent;
        private BigDecimal numStocksStart;
        private Long companyID;

        @Override
        public String toString() {
            return "PorfolioObject{" +
                    "Id=" + Id +
                    ", numStocksCurrent=" + numStocksCurrent +
                    ", numStocksStart=" + numStocksStart +
                    ", companyID=" + companyID +
                    '}';
        }
    }

    /**
     * The type Portfolio value mapper.
     * Used to populate PorfolioObject
     */
    public class PortfolioValueMapper implements RowMapper<PorfolioObject> {
        @Override
        public PorfolioObject mapRow(ResultSet rs, int rowNum) throws SQLException {
            PorfolioObject data = new PorfolioObject();
            data.setId(rs.getLong("id"));
            data.setCompanyID(rs.getLong("companyID"));
            data.setNumStocksStart(rs.getBigDecimal("numStocksStart"));
            data.setNumStocksCurrent(rs.getBigDecimal("numStocksCurrent"));
            return data;
        }
    }

    /**
     * The type Company value mapper.
     * Used to populate Company
     */
    public class CompanyValueMapper implements RowMapper<CompanyValue> {
        @Override
        public CompanyValue mapRow(ResultSet rs, int rowNum) throws SQLException {
            CompanyValue data = new CompanyValue();
            data.setId(rs.getLong("id"));
            data.setCompanyID(rs.getLong("companyID"));
            try {
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                Date tempDate = new java.sql.Date(df.parse(rs.getString("date")).getTime());
                data.setDate(tempDate);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            data.setValue(rs.getBigDecimal("value"));
            return data;
        }
    }

    /**
     * The type Database object mapper.
     * Used to populate overall database structure
     */
    public class DatabaseObjectMapper implements RowMapper<DatabaseObject> {
        @Override
        public DatabaseObject mapRow(ResultSet rs, int rowNum) throws SQLException {
            DatabaseObject data = new DatabaseObject();
            data.setId(rs.getLong("id"));
            data.setCompanyName(rs.getString("companyName"));

            String sql = "SELECT * FROM company WHERE companyID=" + data.Id + " ORDER BY date ASC";
            data.setList(jdbcTemplate.query(sql, new CompanyValueMapper()));

            sql = "SELECT * FROM portfolio WHERE companyID=" + data.Id;
            data.setPorfolio(jdbcTemplate.query(sql, new PortfolioValueMapper()));

            sql = "SELECT * FROM transactions WHERE companyID=" + data.Id + " ORDER BY date ASC";
            data.setTransactions(jdbcTemplate.query(sql, new TransactionsValueMapper()));
            return data;
        }
    }
}
