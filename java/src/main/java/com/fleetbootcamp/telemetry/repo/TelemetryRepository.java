package com.fleetbootcamp.telemetry.repo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetbootcamp.telemetry.dto.AlertView;
import com.fleetbootcamp.telemetry.dto.TelemetryEvent;
import com.fleetbootcamp.telemetry.dto.TelemetryPage;
import com.fleetbootcamp.telemetry.dto.TelemetryView;
import com.fleetbootcamp.telemetry.service.OverspeedRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** All SQL lives here. Always use ? placeholders — never concatenate input into SQL. */
@Repository
public class TelemetryRepository {
    private static final Logger log = LoggerFactory.getLogger(TelemetryRepository.class);

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public TelemetryRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public boolean vehicleExists(String vin) {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM vehicles WHERE vin = ?", Integer.class, vin);
        return n != null && n > 0;
    }

    /**
     * M2: Idempotent insert of a normalised TelemetryEvent.
     * The unique constraint uq_telemetry_vin_ts on (vin, ts_utc) prevents duplicate rows.
     */
    public boolean insert(TelemetryEvent e) {
        String sql = """
                INSERT INTO telemetry (vin, ts_utc, speed_kmph, odometer_km, lat, lon, heading, soc_pct, ignition, source_oem)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (vin, ts_utc) DO NOTHING
                """;
        int rows = jdbc.update(sql,
                e.vin(),
                e.tsUtc().atOffset(ZoneOffset.UTC),
                e.speedKmph(),
                e.odometerKm(),
                e.lat(),
                e.lon(),
                e.heading(),
                e.socPct(),
                e.ignition() != null ? e.ignition() : Boolean.TRUE,
                e.sourceOem()
        );
        return rows > 0;
    }

    /**
     * M3: Find latest telemetry ping for vehicle.
     */
    public Optional<TelemetryView> findLatest(String vin) {
        String sql = """
                SELECT vin, ts_utc, speed_kmph, odometer_km, lat, lon, heading, soc_pct, ignition, source_oem
                FROM telemetry
                WHERE vin = ?
                ORDER BY ts_utc DESC
                LIMIT 1
                """;
        try {
            TelemetryView view = jdbc.queryForObject(sql, this::mapTelemetryRow, vin);
            return Optional.ofNullable(view);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * M3: Keyset pagination on ts_utc (no OFFSET) using index on (vin, ts_utc).
     */
    public TelemetryPage findRange(String vin, OffsetDateTime from, OffsetDateTime to, int limit, String cursor) {
        int pageSize = Math.max(1, Math.min(limit, 1000));
        StringBuilder sql = new StringBuilder("""
                SELECT vin, ts_utc, speed_kmph, odometer_km, lat, lon, heading, soc_pct, ignition, source_oem
                FROM telemetry
                WHERE vin = ?
                """);

        List<Object> params = new ArrayList<>();
        params.add(vin);

        if (from != null) {
            sql.append(" AND ts_utc >= ?");
            params.add(from);
        }
        if (to != null) {
            sql.append(" AND ts_utc <= ?");
            params.add(to);
        }
        if (cursor != null && !cursor.isBlank()) {
            OffsetDateTime cursorTs = OffsetDateTime.parse(cursor.trim());
            sql.append(" AND ts_utc > ?");
            params.add(cursorTs);
        }

        sql.append(" ORDER BY ts_utc ASC LIMIT ?");
        params.add(pageSize + 1); // fetch 1 extra to check for next page

        List<TelemetryView> rows = jdbc.query(sql.toString(), this::mapTelemetryRow, params.toArray());

        String nextCursor = null;
        List<TelemetryView> data;
        if (rows.size() > pageSize) {
            data = rows.subList(0, pageSize);
            nextCursor = data.get(pageSize - 1).tsUtc().toString();
        } else {
            data = rows;
        }

        return new TelemetryPage(data, nextCursor);
    }

    /**
     * M3: Retrieve alerts for a VIN, newest first.
     */
    public List<AlertView> findAlerts(String vin) {
        String sql = """
                SELECT id, vin, type, started_at, details, created_at
                FROM alerts
                WHERE vin = ?
                ORDER BY created_at DESC, id DESC
                """;
        return jdbc.query(sql, this::mapAlertRow, vin);
    }

    /**
     * M3: Retrieve latest position of every active vehicle in a customer's fleet in ONE SQL query.
     */
    public List<TelemetryView> findFleetLatest(int customerId) {
        String sql = """
                SELECT DISTINCT ON (v.vin)
                    v.vin, t.ts_utc, t.speed_kmph, t.odometer_km, t.lat, t.lon, t.heading, t.soc_pct, t.ignition, t.source_oem
                FROM vehicles v
                JOIN telemetry t ON v.vin = t.vin
                WHERE v.customer_id = ? AND v.status = 'active'
                ORDER BY v.vin, t.ts_utc DESC
                """;
        return jdbc.query(sql, this::mapTelemetryRow, customerId);
    }

    /**
     * M3: Fetch the most recent speed pings for a VIN to evaluate consecutive overspeed rules.
     */
    public List<OverspeedRule.SpeedPing> findRecentSpeedPings(String vin, int limit) {
        String sql = """
                SELECT ts_utc, speed_kmph
                FROM telemetry
                WHERE vin = ?
                ORDER BY ts_utc DESC
                LIMIT ?
                """;
        return jdbc.query(sql, (rs, rowNum) -> {
            Timestamp ts = rs.getTimestamp("ts_utc");
            OffsetDateTime tsUtc = ts != null ? ts.toInstant().atOffset(ZoneOffset.UTC) : null;
            return new OverspeedRule.SpeedPing(tsUtc, rs.getDouble("speed_kmph"));
        }, vin, limit);
    }

    /**
     * M3: Idempotent insert of an overspeed alert row.
     * The table constraint uq_alert UNIQUE (vin, type, started_at) prevents duplicate alerts per run.
     */
    public boolean insertAlert(String vin, String type, OffsetDateTime startedAt, Map<String, Object> details) {
        String json = "{}";
        try {
            if (details != null) {
                json = objectMapper.writeValueAsString(details);
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize alert details to JSON: {}", e.getMessage());
        }

        String sql = """
                INSERT INTO alerts (vin, type, started_at, details)
                VALUES (?, ?, ?, ?::jsonb)
                ON CONFLICT (vin, type, started_at) DO NOTHING
                """;
        int rows = jdbc.update(sql, vin, type, startedAt, json);
        return rows > 0;
    }

    private TelemetryView mapTelemetryRow(ResultSet rs, int rowNum) throws SQLException {
        Timestamp ts = rs.getTimestamp("ts_utc");
        OffsetDateTime tsUtc = ts != null ? ts.toInstant().atOffset(ZoneOffset.UTC) : null;
        Double odometerKm = rs.getObject("odometer_km") != null ? rs.getDouble("odometer_km") : null;
        Integer heading = rs.getObject("heading") != null ? rs.getInt("heading") : null;
        Double socPct = rs.getObject("soc_pct") != null ? rs.getDouble("soc_pct") : null;
        Boolean ignition = rs.getObject("ignition") != null ? rs.getBoolean("ignition") : null;

        return new TelemetryView(
                rs.getString("vin").trim(),
                tsUtc,
                rs.getDouble("speed_kmph"),
                odometerKm,
                rs.getDouble("lat"),
                rs.getDouble("lon"),
                heading,
                socPct,
                ignition,
                rs.getString("source_oem")
        );
    }

    private AlertView mapAlertRow(ResultSet rs, int rowNum) throws SQLException {
        Timestamp startedAtTs = rs.getTimestamp("started_at");
        OffsetDateTime startedAt = startedAtTs != null ? startedAtTs.toInstant().atOffset(ZoneOffset.UTC) : null;
        Timestamp createdAtTs = rs.getTimestamp("created_at");
        OffsetDateTime createdAt = createdAtTs != null ? createdAtTs.toInstant().atOffset(ZoneOffset.UTC) : null;

        String rawJson = rs.getString("details");
        Map<String, Object> details = Collections.emptyMap();
        if (rawJson != null && !rawJson.isBlank()) {
            try {
                details = objectMapper.readValue(rawJson, new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialize alert details JSON: {}", e.getMessage());
            }
        }

        return new AlertView(
                rs.getLong("id"),
                rs.getString("vin").trim(),
                rs.getString("type"),
                startedAt,
                details,
                createdAt
        );
    }
}
