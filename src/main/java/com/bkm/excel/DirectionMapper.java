package com.bkm.excel;

public final class DirectionMapper {
    private DirectionMapper() {
    }

    public static String toMeteringDirection(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().toLowerCase();
        if ("aufsteigend".equals(value)) {
            return "ASCENDING";
        }
        if ("absteigend".equals(value)) {
            return "DESCENDING";
        }
        if ("beides".equals(value)) {
            return "BOTH";
        }
        return null;
    }
}
