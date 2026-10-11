package com.bkm.excel;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExcelProcessor {
    private final AppConfig config;
    private final DataFormatter formatter = new DataFormatter();

    public ExcelProcessor(AppConfig config) {
        this.config = config;
    }

    public Path process() throws IOException {
        Path input = Paths.get(config.require("excel.input"));
        if (!Files.isRegularFile(input)) {
            throw new IOException("Excel-Datei nicht gefunden: " + input.toAbsolutePath());
        }
        Path output = resolveOutput(input);

        GeoServiceClient geoClient = new GeoServiceClient(config);
        String token = new KeycloakTokenClient(config).fetchAccessToken();

        Workbook workbook;
        try {
            workbook = WorkbookFactory.create(input.toFile());
        } catch (Exception ex) {
            throw new IOException("Excel-Datei konnte nicht gelesen werden: " + input.toAbsolutePath(), ex);
        }

        try {
            Sheet sheet = selectSheet(workbook);
            HeaderMap headers = HeaderMap.from(sheet, config, formatter);
            List<ConversionRequest> requests = collectRequests(sheet, headers);
            System.out.println("Zu konvertierende Zeilen: " + requests.size());

            if (!requests.isEmpty()) {
                List<JsonNode> results = geoClient.convertLocations(token, requests);
                if (results.size() != requests.size()) {
                    throw new IOException("Antwortlaenge " + results.size()
                            + " weicht von Anfragelaenge " + requests.size() + " ab.");
                }
                applyResults(sheet, headers, requests, results);
            }

            Files.createDirectories(output.getParent() == null ? Paths.get(".") : output.getParent());
            try (java.io.OutputStream out = Files.newOutputStream(output)) {
                workbook.write(out);
            }
        } finally {
            workbook.close();
        }
        return output;
    }

    private List<ConversionRequest> collectRequests(Sheet sheet, HeaderMap headers) {
        List<ConversionRequest> requests = new ArrayList<ConversionRequest>();
        int last = sheet.getLastRowNum();
        for (int rowIndex = headers.headerRowIndex + 1; rowIndex <= last; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            String directionRaw = cellText(row, headers.richtung);
            String direction = DirectionMapper.toMeteringDirection(directionRaw);
            if (direction == null) {
                continue;
            }
            if (!isBlank(row, headers.lat) || !isBlank(row, headers.lon)) {
                continue;
            }
            BigDecimal bkm = BkmParser.parseKilometers(cellText(row, headers.bkm));
            if (bkm == null) {
                continue;
            }
            String road = cellText(row, headers.road).trim();
            if (road.isEmpty()) {
                System.out.println("Zeile " + (rowIndex + 1) + ": Strasse/BAB fehlt, uebersprungen.");
                continue;
            }
            String block = cellText(row, headers.block).trim();
            requests.add(new ConversionRequest(rowIndex, road, BkmParser.toStartMeter(bkm), direction,
                    block.isEmpty() ? null : block));
        }
        return requests;
    }

    private void applyResults(Sheet sheet, HeaderMap headers, List<ConversionRequest> requests, List<JsonNode> results) {
        for (int i = 0; i < requests.size(); i++) {
            ConversionRequest request = requests.get(i);
            JsonNode result = results.get(i);
            Row row = sheet.getRow(request.getRowIndex());
            LatLon latLon = GeoServiceClient.extractLatLon(result);
            if (latLon == null) {
                System.out.println("Zeile " + (request.getRowIndex() + 1) + ": Konvertierung fehlgeschlagen: "
                        + GeoServiceClient.errorMessage(result));
                continue;
            }
            setNumeric(row, headers.lat, latLon.getLat());
            setNumeric(row, headers.lon, latLon.getLon());
        }
    }

    private Sheet selectSheet(Workbook workbook) {
        String name = config.get("excel.sheet");
        if (!name.isEmpty()) {
            Sheet sheet = workbook.getSheet(name);
            if (sheet == null) {
                throw new IllegalArgumentException("Blatt nicht gefunden: " + name);
            }
            return sheet;
        }
        return workbook.getSheetAt(0);
    }

    private Path resolveOutput(Path input) {
        String configured = config.get("excel.output");
        if (!configured.isEmpty()) {
            return Paths.get(configured);
        }
        String fileName = input.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String base = dot < 0 ? fileName : fileName.substring(0, dot);
        String ext = dot < 0 ? ".xlsx" : fileName.substring(dot);
        Path parent = input.getParent();
        Path named = Paths.get(base + "_converted" + ext);
        return parent == null ? named : parent.resolve(named);
    }

    private String cellText(Row row, int column) {
        if (column < 0) {
            return "";
        }
        Cell cell = row.getCell(column);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private boolean isBlank(Row row, int column) {
        return cellText(row, column).isEmpty();
    }

    private static void setNumeric(Row row, int column, BigDecimal value) {
        Cell cell = row.getCell(column);
        if (cell == null) {
            cell = row.createCell(column, CellType.NUMERIC);
        }
        cell.setCellValue(value.doubleValue());
    }

    static class HeaderMap {
        final int headerRowIndex;
        final int richtung;
        final int lat;
        final int lon;
        final int bkm;
        final int road;
        final int block;

        HeaderMap(int headerRowIndex, int richtung, int lat, int lon, int bkm, int road, int block) {
            this.headerRowIndex = headerRowIndex;
            this.richtung = richtung;
            this.lat = lat;
            this.lon = lon;
            this.bkm = bkm;
            this.road = road;
            this.block = block;
        }

        static HeaderMap from(Sheet sheet, AppConfig config, DataFormatter formatter) {
            String richtungName = config.get("excel.column.richtung");
            String latName = config.get("excel.column.lat");
            String lonName = config.get("excel.column.lon");
            String bkmName = config.get("excel.column.bkm");
            String roadName = config.get("excel.column.road");
            String blockName = config.get("excel.column.block");
            if (blockName.isEmpty()) {
                blockName = "Block:";
            }

            int last = Math.min(sheet.getLastRowNum(), 20);
            for (int rowIndex = 0; rowIndex <= last; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                Map<String, Integer> headers = new HashMap<String, Integer>();
                short lastCell = row.getLastCellNum();
                for (int col = 0; col < lastCell; col++) {
                    Cell cell = row.getCell(col);
                    if (cell == null) {
                        continue;
                    }
                    String text = formatter.formatCellValue(cell).trim();
                    if (!text.isEmpty()) {
                        headers.put(normalize(text), col);
                    }
                }
                Integer richtung = find(headers, richtungName);
                Integer lat = find(headers, latName);
                Integer lon = find(headers, lonName);
                Integer bkm = find(headers, bkmName);
                Integer road = find(headers, roadName);
                Integer block = find(headers, blockName);
                if (richtung != null && lat != null && lon != null && bkm != null && road != null) {
                    return new HeaderMap(rowIndex, richtung, lat, lon, bkm, road,
                            block == null ? -1 : block.intValue());
                }
            }
            throw new IllegalArgumentException(
                    "Spalten nicht gefunden. Erwartet: " + richtungName + ", " + latName + ", "
                            + lonName + ", " + bkmName + ", " + roadName);
        }

        private static Integer find(Map<String, Integer> headers, String name) {
            Integer exact = headers.get(normalize(name));
            if (exact != null) {
                return exact;
            }
            return headers.get(normalize(name.replace(":", "")));
        }

        private static String normalize(String value) {
            return value.trim().toLowerCase().replace(":", "").replaceAll("\\s+", " ");
        }
    }
}
