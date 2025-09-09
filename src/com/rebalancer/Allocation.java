package com.rebalancer;

import java.math.BigDecimal;

public class Allocation {
    private final String security;
    private final BigDecimal percent;

    public Allocation(String security, BigDecimal percent) {
        this.security = security;
        this.percent = percent;
    }

    public String getSecurity() {
        return security;
    }

    public BigDecimal getPercent() {
        return percent;
    }
}
