package com.bkm.excel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class GeoServiceClient {
    private final AppConfig config;
    private final ObjectMapper mapper = new ObjectMapper();

    public GeoServiceClient(AppConfig config) {
        this.config = config;
    }

    public List<JsonNode> convertLocations(String accessToken, List<ConversionRequest> requests) throws IOException {
        ObjectNode body = mapper.createObjectNode();
        body.put("targetType", config.get("geo.target-type").isEmpty()
                ? "GeographicalReference"
                : config.get("geo.target-type"));
        ArrayNode locations = body.putArray("locations");
        String type = config.get("geo.location-type");
        if (type.isEmpty()) {
            type = "de.e21x.data.location.KilometerReference$Bean";
        }
        for (ConversionRequest request : requests) {
            ObjectNode location = locations.addObject();
            location.put("@type", type);
            location.put("roadNumber", request.getRoadNumber());
            location.put("startMeter", request.getStartMeter());
            location.put("direction", request.getDirection());
            if (request.getStartBlockNumber() != null && !request.getStartBlockNumber().isEmpty()) {
                location.put("startBlockNumber", request.getStartBlockNumber());
            }
        }

        String url = trimSlash(config.require("geo.base-url"))
                + (config.get("geo.convert-path").isEmpty() ? "/locations" : config.get("geo.convert-path"));
        byte[] payload = mapper.writeValueAsBytes(body);

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        try (OutputStream out = connection.getOutputStream()) {
            out.write(payload);
        }

        int status = connection.getResponseCode();
        InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        JsonNode json = mapper.readTree(stream == null ? "[]".getBytes(StandardCharsets.UTF_8) : readAll(stream));
        if (status >= 400) {
            throw new IOException("convertLocations fehlgeschlagen (" + status + "): " + json);
        }
        if (!json.isArray()) {
            throw new IOException("Unerwartete convertLocations-Antwort: " + json);
        }

        List<JsonNode> result = new ArrayList<JsonNode>();
        for (JsonNode node : json) {
            result.add(node);
        }
        return result;
    }

    public static LatLon extractLatLon(JsonNode location) {
        if (location == null || location.isNull()) {
            return null;
        }
        String type = text(location, "@type");
        if (type != null && type.contains("LocationError")) {
            return null;
        }
        JsonNode geo = location;
        if (location.has("geo")) {
            geo = location.get("geo");
        }
        JsonNode geometry = geo.get("geometry");
        if (geometry == null || geometry.isNull()) {
            return null;
        }
        JsonNode coordinates = geometry.get("coordinates");
        if (coordinates == null || !coordinates.isArray() || coordinates.size() < 2) {
            return null;
        }
        if (coordinates.get(0).isArray()) {
            coordinates = coordinates.get(0);
        }
        BigDecimal lon = coordinates.get(0).decimalValue();
        BigDecimal lat = coordinates.get(1).decimalValue();
        return new LatLon(lat, lon);
    }

    public static String errorMessage(JsonNode location) {
        if (location == null) {
            return "leere Antwort";
        }
        JsonNode description = location.get("errorDescription");
        if (description != null && !description.asText().isEmpty()) {
            return description.asText();
        }
        return location.toString();
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static String trimSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static byte[] readAll(InputStream stream) throws IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int read;
        while ((read = stream.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }
}
