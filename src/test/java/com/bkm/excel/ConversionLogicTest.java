package com.bkm.excel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ConversionLogicTest {
    @Test
    public void mapsGermanDirections() {
        assertEquals("ASCENDING", DirectionMapper.toMeteringDirection("aufsteigend"));
        assertEquals("DESCENDING", DirectionMapper.toMeteringDirection(" Absteigend "));
        assertEquals("BOTH", DirectionMapper.toMeteringDirection("beides"));
        assertNull(DirectionMapper.toMeteringDirection("links"));
    }

    @Test
    public void parsesGermanDecimalBkmToMeters() {
        BigDecimal km = BkmParser.parseKilometers("12,345");
        assertNotNull(km);
        assertEquals(12345, BkmParser.toStartMeter(km));
    }

    @Test
    public void extractsPointCoordinatesFromGeoJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode location = mapper.createObjectNode();
        location.put("@type", "de.e21x.data.location.GeographicalReference$Bean");
        ObjectNode geometry = location.putObject("geometry");
        geometry.put("type", "Point");
        ArrayNode coordinates = geometry.putArray("coordinates");
        coordinates.add(8.123);
        coordinates.add(50.456);
        LatLon latLon = GeoServiceClient.extractLatLon(location);
        assertNotNull(latLon);
        assertEquals(new BigDecimal("50.456"), latLon.getLat());
        assertEquals(new BigDecimal("8.123"), latLon.getLon());
    }
}
