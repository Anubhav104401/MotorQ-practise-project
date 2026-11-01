package com.fleetbootcamp.telemetry.service;

import com.fleetbootcamp.telemetry.dto.HeartbeatRequest;
import com.fleetbootcamp.telemetry.dto.TelemetryEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NormalizerTest {

    @Test
    void normalizesKmOdometerAndUppercasesVin() {
        OffsetDateTime ts = OffsetDateTime.of(2026, 10, 28, 10, 0, 0, 0, ZoneOffset.UTC);
        HeartbeatRequest request = new HeartbeatRequest(
                "1fa6cvncncxw927tc",
                ts,
                55.0,
                10000.0,
                "km",
                12.97,
                77.59,
                90,
                50.0,
                true,
                "OEM-A"
        );

        TelemetryEvent event = Normalizer.normalize(request);

        assertEquals("1FA6CVNCNCXW927TC", event.vin());
        assertEquals(TelemetryEvent.SCHEMA_V1, event.schema());
        assertEquals(10000.0, event.odometerKm());
        assertEquals(Instant.parse("2026-10-28T10:00:00Z"), event.tsUtc());
        assertEquals(55.0, event.speedKmph());
        assertEquals(12.97, event.lat());
        assertEquals(77.59, event.lon());
        assertEquals(90, event.heading());
        assertEquals(50.0, event.socPct());
        assertTrue(event.ignition());
        assertEquals("OEM-A", event.sourceOem());
    }

    @Test
    void normalizesMilesOdometerToKm() {
        OffsetDateTime ts = OffsetDateTime.of(2026, 10, 28, 10, 0, 0, 0, ZoneOffset.UTC);
        HeartbeatRequest request = new HeartbeatRequest(
                "1FA6CVNCNCXW927TC",
                ts,
                60.0,
                100.0,
                "mi",
                12.97,
                77.59,
                null,
                null,
                true,
                "OEM-B"
        );

        TelemetryEvent event = Normalizer.normalize(request);

        assertEquals(160.9344, event.odometerKm(), 0.0001);
    }

    @Test
    void nullOdometerUnitDefaultsToKm() {
        OffsetDateTime ts = OffsetDateTime.of(2026, 10, 28, 10, 0, 0, 0, ZoneOffset.UTC);
        HeartbeatRequest request = new HeartbeatRequest(
                "1FA6CVNCNCXW927TC",
                ts,
                60.0,
                12345.0,
                null,
                12.97,
                77.59,
                null,
                null,
                null,
                "OEM-A"
        );

        TelemetryEvent event = Normalizer.normalize(request);

        assertEquals(12345.0, event.odometerKm());
        assertTrue(event.ignition());
    }

    @Test
    void normalizesIstTimestampToUtcInstant() {
        OffsetDateTime ist = OffsetDateTime.parse("2026-10-28T15:30:00+05:30");
        HeartbeatRequest request = new HeartbeatRequest(
                "1FA6CVNCNCXW927TC",
                ist,
                50.0,
                1000.0,
                "km",
                12.97,
                77.59,
                null,
                null,
                true,
                "OEM-A"
        );

        TelemetryEvent event = Normalizer.normalize(request);

        assertEquals(Instant.parse("2026-10-28T10:00:00Z"), event.tsUtc());
    }

    @Test
    void throwsOnUnknownOdometerUnit() {
        OffsetDateTime ts = OffsetDateTime.of(2026, 10, 28, 10, 0, 0, 0, ZoneOffset.UTC);
        HeartbeatRequest request = new HeartbeatRequest(
                "1FA6CVNCNCXW927TC",
                ts,
                50.0,
                1000.0,
                "furlong",
                12.97,
                77.59,
                null,
                null,
                true,
                "OEM-A"
        );

        assertThrows(IllegalArgumentException.class, () -> Normalizer.normalize(request));
    }
}
