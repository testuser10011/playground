package com.playground.service;

import com.playground.dataobject.StockObject;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import yahoofinance.YahooFinance;

import java.io.IOException;
import java.math.BigDecimal;

@AllArgsConstructor
@Service
public class ReaderService {

    public StockObject stockGetter(final String companySymbol) {
        try {
            return new StockObject(YahooFinance.get(companySymbol));
        } catch (IOException e) {
            System.out.println(e);
            return null;
        }
    }

    public BigDecimal getStockValueNow(final StockObject thisStock, boolean refreshNow) throws IOException {
        return thisStock.getActualStock().getQuote(refreshNow).getPrice();
    }
}
