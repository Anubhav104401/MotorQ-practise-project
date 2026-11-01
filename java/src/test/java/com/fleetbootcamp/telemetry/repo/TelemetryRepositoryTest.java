package com.fleetbootcamp.telemetry.repo;

import com.fleetbootcamp.telemetry.dto.TelemetryEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelemetryRepositoryTest {

    @Mock
    private JdbcTemplate jdbc;

    private TelemetryRepository repository;

    @BeforeEach
    void setUp() {
        repository = new TelemetryRepository(jdbc);
    }

    @Test
    void insertReturnsTrueWhenNewRowInserted() {
        TelemetryEvent event = new TelemetryEvent(
                TelemetryEvent.SCHEMA_V1,
                "1FA6CVNCNCXW927TC",
                Instant.parse("2026-10-28T10:00:00Z"),
                50.0,
                10000.0,
                12.97,
                77.59,
                90,
                80.0,
                true,
                "OEM-A"
        );

        when(jdbc.update(
                anyString(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(1);

        boolean result = repository.insert(event);

        assertTrue(result);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(
                sqlCaptor.capture(),
                eq("1FA6CVNCNCXW927TC"),
                eq(OffsetDateTime.of(2026, 10, 28, 10, 0, 0, 0, ZoneOffset.UTC)),
                eq(50.0),
                eq(10000.0),
                eq(12.97),
                eq(77.59),
                eq(90),
                eq(80.0),
                eq(true),
                eq("OEM-A")
        );
        assertTrue(sqlCaptor.getValue().contains("ON CONFLICT (vin, ts_utc) DO NOTHING"));
    }

    @Test
    void insertReturnsFalseWhenDuplicateIgnored() {
        TelemetryEvent event = new TelemetryEvent(
                TelemetryEvent.SCHEMA_V1,
                "1FA6CVNCNCXW927TC",
                Instant.parse("2026-10-28T10:00:00Z"),
                50.0,
                10000.0,
                12.97,
                77.59,
                null,
                null,
                null,
                "OEM-A"
        );

        when(jdbc.update(
                anyString(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(0);

        boolean result = repository.insert(event);

        assertFalse(result);
    }
}
