package com.bkm.excel;

import java.nio.file.Path;

public class ExcelBkmGeoApp {
    public static void main(String[] args) {
        try {
            AppConfig config = AppConfig.load(args);
            Path output = new ExcelProcessor(config).process();
            System.out.println("Fertig. Ausgabe: " + output.toAbsolutePath());
        } catch (Exception ex) {
            System.err.println("Fehler: " + ex.getMessage());
            System.exit(1);
        }
    }
}
