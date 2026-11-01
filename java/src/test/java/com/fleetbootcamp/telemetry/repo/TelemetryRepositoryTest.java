package com.fleetbootcamp.telemetry.repo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetbootcamp.telemetry.dto.TelemetryEvent;
import com.fleetbootcamp.telemetry.dto.TelemetryPage;
import com.fleetbootcamp.telemetry.dto.TelemetryView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    private ObjectMapper objectMapper;
    private TelemetryRepository repository;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        repository = new TelemetryRepository(jdbc, objectMapper);
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

    @Test
    void findLatestReturnsEmptyWhenNoRow() {
        when(jdbc.queryForObject(anyString(), any(RowMapper.class), eq("1FA6CVNCNCXW927TC")))
                .thenThrow(new EmptyResultDataAccessException(1));

        Optional<TelemetryView> result = repository.findLatest("1FA6CVNCNCXW927TC");
        assertTrue(result.isEmpty());
    }

    @Test
    void findRangeReturnsKeysetPaginationNextCursor() {
        OffsetDateTime t1 = OffsetDateTime.parse("2026-10-28T10:00:00Z");
        OffsetDateTime t2 = OffsetDateTime.parse("2026-10-28T10:05:00Z");
        OffsetDateTime t3 = OffsetDateTime.parse("2026-10-28T10:10:00Z");

        TelemetryView v1 = new TelemetryView("1FA6CVNCNCXW927TC", t1, 50.0, 100.0, 12.0, 77.0, 90, null, true, "OEM-A");
        TelemetryView v2 = new TelemetryView("1FA6CVNCNCXW927TC", t2, 55.0, 105.0, 12.1, 77.1, 90, null, true, "OEM-A");
        TelemetryView v3 = new TelemetryView("1FA6CVNCNCXW927TC", t3, 60.0, 110.0, 12.2, 77.2, 90, null, true, "OEM-A");

        // Requested limit = 2, so query fetches up to 3 rows
        when(jdbc.query(anyString(), any(RowMapper.class), any(), any()))
                .thenReturn(List.of(v1, v2, v3));

        TelemetryPage page = repository.findRange("1FA6CVNCNCXW927TC", null, null, 2, null);

        assertEquals(2, page.data().size());
        assertEquals(t2.toString(), page.nextCursor());
    }

    @Test
    void findRangeReturnsNullNextCursorOnLastPage() {
        OffsetDateTime t1 = OffsetDateTime.parse("2026-10-28T10:00:00Z");
        TelemetryView v1 = new TelemetryView("1FA6CVNCNCXW927TC", t1, 50.0, 100.0, 12.0, 77.0, 90, null, true, "OEM-A");

        when(jdbc.query(anyString(), any(RowMapper.class), any(), any()))
                .thenReturn(List.of(v1));

        TelemetryPage page = repository.findRange("1FA6CVNCNCXW927TC", null, null, 10, null);

        assertEquals(1, page.data().size());
        assertNull(page.nextCursor());
    }

    @Test
    void insertAlertReturnsTrueWhenNewAlert() {
        OffsetDateTime start = OffsetDateTime.parse("2026-10-28T10:00:00Z");
        Map<String, Object> details = Map.of("speed_kmph", 115.0);

        when(jdbc.update(anyString(), eq("1FA6CVNCNCXW927TC"), eq("OVERSPEED"), eq(start), anyString()))
                .thenReturn(1);

        boolean inserted = repository.insertAlert("1FA6CVNCNCXW927TC", "OVERSPEED", start, details);
        assertTrue(inserted);
    }
}
