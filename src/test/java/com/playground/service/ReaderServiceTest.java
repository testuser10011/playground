package com.playground.service;

import com.playground.ServiceApp;
import com.playground.dataobject.StockObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The type Reader service test.
 */
@SpringBootTest(classes = ServiceApp.class)
class ReaderServiceTest implements CommandLineRunner {

    @Autowired
    private ReaderService srv;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
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
                "    id        INTEGER PRIMARY KEY\n" +
                "                      NOT NULL,\n" +
                "    numStocks BIGINT  DEFAULT (0),\n" +
                "    companyID INTEGER NOT NULL\n" +
                "                      REFERENCES companiesCatalogue (id) \n" +
                "                      UNIQUE ON CONFLICT IGNORE\n" +
                ");");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS transactions (\n" +
                "    id            INTEGER  PRIMARY KEY AUTOINCREMENT,\n" +
                "    company       INTEGER  REFERENCES company (id) \n" +
                "                           NOT NULL,\n" +
                "    price         DECIMAL,\n" +
                "    [transaction] STRING   NOT NULL,\n" +
                "    date          DATETIME NOT NULL\n" +
                ");");

    }

    @Test
    void invoke() throws Exception {
        final StockObject object = srv.stockGetter("TSLA");
        System.out.println(object.getActualStock());

        /* populate database with historical data */
        final StockObject object1 = srv.stockGetterOnDate("TSLA", "11-06-2021");
        final StockObject object2 = srv.stockGetterOnDate("AMZN", "11-06-2021");

        srv.decodeStock(object1, jdbcTemplate, true);
        srv.decodeStock(object2, jdbcTemplate, true);
        srv.serveRequest(object1, jdbcTemplate);
        srv.serveRequest(object2, jdbcTemplate);
    }


    @Override
    public void run(String... args) throws Exception {

    }


    @AfterEach
    void tearDown() {
        System.out.println("Finishing test");
        /*
        jdbcTemplate.execute("drop table companiesCatalogue;");
        jdbcTemplate.execute("drop table company;");
        jdbcTemplate.execute("drop table portfolio;");
        jdbcTemplate.execute("drop table transactions;");
        //jdbcTemplate = null;
        */
    }
}