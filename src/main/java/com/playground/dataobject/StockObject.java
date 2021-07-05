package com.playground.dataobject;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import yahoofinance.Stock;

import java.time.LocalDateTime;

@Getter
@With
@AllArgsConstructor
public class StockObject {
    private final Stock actualStock;
    private final LocalDateTime dateAccessed;

    public StockObject(Stock stock) {
        this.actualStock = stock;
        dateAccessed = LocalDateTime.now();
    }


}
