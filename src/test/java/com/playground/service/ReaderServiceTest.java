package com.playground.service;

import com.playground.dataobject.StockObject;
import com.playground.finance.ServiceApp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = ServiceApp.class)
class ReaderServiceTest {

    @Autowired
    private ReaderService srv;

    @Test
    void invoke() {
        final StockObject object = srv.stockGetter("UU.L");
        System.out.println(object.getActualStock());
    }
}