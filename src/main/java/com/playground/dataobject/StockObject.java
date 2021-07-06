package com.playground.dataobject;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.With;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import yahoofinance.Stock;
import yahoofinance.histquotes.HistoricalQuote;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Getter
@With
@AllArgsConstructor
public class StockObject {
    private final Stock actualStock;
    private final LocalDateTime dateAccessed;
    private HistoricalQuote historicalQuote;


    private JdbcTemplate jdbcTemplate;

    public StockObject(Stock stock) {
        this.actualStock = stock;
        dateAccessed = LocalDateTime.now();
    }

    public List<DatabaseObject> getCompanyValues(JdbcTemplate jdbcTemplate) {
        String sql = "SELECT * FROM companiesCatalogue";
        this.jdbcTemplate = jdbcTemplate;
        return jdbcTemplate.query(sql, new DatabaseObjectMapper());
    }

    private enum Transact {SELL, BUY}

    @Getter
    @Setter
    private class TransactionsObject {
        private Long Id;
        private BigDecimal numStocks;
        private Long companyID;
        private Transact transaction;
        private Date date;
    }

    @Getter
    @Setter
    private class CompanyValue {
        private Long id;
        private Long companyID;
        private Date date;
        private BigDecimal value;
    }

    @Getter
    @Setter
    public class DatabaseObject {
        private Long Id;
        private String companyName;
        private List<CompanyValue> list;
        private List<PorfolioObject> porfolio;
    }

    @Getter
    @Setter
    private class PorfolioObject {
        private Long Id;
        private BigDecimal numStocks;
        private Long companyID;

    }

    public class PortfolioValueMapper implements RowMapper<PorfolioObject> {
        @Override
        public PorfolioObject mapRow(ResultSet rs, int rowNum) throws SQLException {
            PorfolioObject data = new PorfolioObject();
            data.setId(rs.getLong("id"));
            data.setCompanyID(rs.getLong("companyID"));
            data.setNumStocks(rs.getBigDecimal("numStocks"));
            return data;
        }
    }

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

    public class DatabaseObjectMapper implements RowMapper<DatabaseObject> {
        @Override
        public DatabaseObject mapRow(ResultSet rs, int rowNum) throws SQLException {
            DatabaseObject data = new DatabaseObject();
            data.setId(rs.getLong("id"));
            data.setCompanyName(rs.getString("companyName"));

            String sql = "SELECT * FROM company WHERE companyID=" + data.Id;
            data.setList(jdbcTemplate.query(sql, new CompanyValueMapper()));

            sql = "SELECT * FROM portfolio WHERE companyID=" + data.Id;
            data.setPorfolio(jdbcTemplate.query(sql, new PortfolioValueMapper()));
            return data;
        }
    }
}
