package com.bkm.excel;

import java.math.BigDecimal;

public class LatLon {
    private final BigDecimal lat;
    private final BigDecimal lon;

    public LatLon(BigDecimal lat, BigDecimal lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public BigDecimal getLat() {
        return lat;
    }

    public BigDecimal getLon() {
        return lon;
    }
}
