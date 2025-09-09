package com.rebalancer;

import java.math.BigDecimal;

public class TargetLine {
    private final String security;
    private final BigDecimal price;
    private final long currentQty;
    private final long targetQty;
    private final BigDecimal targetPercent;

    public TargetLine(String security, BigDecimal price, long currentQty, long targetQty, BigDecimal targetPercent) {
        this.security = security;
        this.price = price;
        this.currentQty = currentQty;
        this.targetQty = targetQty;
        this.targetPercent = targetPercent;
    }

    public String getSecurity() {
        return security;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public long getCurrentQty() {
        return currentQty;
    }

    public long getTargetQty() {
        return targetQty;
    }

    public BigDecimal getTargetPercent() {
        return targetPercent;
    }
}
