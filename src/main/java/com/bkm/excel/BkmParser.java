package com.bkm.excel;

import java.math.BigDecimal;

public final class BkmParser {
    private BkmParser() {
    }

    public static BigDecimal parseKilometers(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim();
        if (text.isEmpty()) {
            return null;
        }
        text = text.replace(" ", "").replace("km", "").replace("KM", "");
        if (text.contains(",") && text.contains(".")) {
            text = text.replace(".", "").replace(',', '.');
        } else {
            text = text.replace(',', '.');
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static int toStartMeter(BigDecimal kilometers) {
        return kilometers.multiply(BigDecimal.valueOf(1000)).intValue();
    }
}
