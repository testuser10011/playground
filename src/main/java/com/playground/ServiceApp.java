package com.playground;


import com.playground.service.ReaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;


/**
 * The type Service app.
 * currently just used in Spring boot beans
 */
@SpringBootApplication
public class ServiceApp {
    @Autowired
    private ReaderService srv;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public static void main(String[] args) {
        SpringApplication.run(ServiceApp.class, args);
    }
}

