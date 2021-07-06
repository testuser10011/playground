package com.playground;

import com.playground.service.ReaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class ServiceApp {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReaderService srv;

    public static void main(String[] args) {
        SpringApplication.run(ServiceApp.class, args);
    }
}

