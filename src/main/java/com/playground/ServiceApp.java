package com.playground;


import com.playground.service.ReaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;


/**
 * The type Service app.
 * currently just used in Spring boot beans
 */
@SpringBootApplication
public class ServiceApp {
    @Autowired
    private ReaderService srv;

    public static void main(String[] args) throws IOException {
        String confFile = "appConfig.xml";
        ApplicationContext context = new ClassPathXmlApplicationContext(confFile);
        ReaderService srv;
        srv = (ReaderService) context.getBean("readerService");
        srv.decodeStock(true);
        srv.serveRequest();
        srv.tearDown();
    }
}

