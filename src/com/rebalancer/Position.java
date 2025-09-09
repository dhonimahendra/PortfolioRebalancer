package com.rebalancer;

import java.math.BigDecimal;

public class Position {
    private final String security;
    private final BigDecimal price;
    private final long quantity;

    public Position(String security, BigDecimal price, long quantity) {
        this.security = security;
        this.price = price;
        this.quantity = quantity;
    }

    public String getSecurity() {
        return security;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public long getQuantity() {
        return quantity;
    }

    public BigDecimal getMarketValue() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
