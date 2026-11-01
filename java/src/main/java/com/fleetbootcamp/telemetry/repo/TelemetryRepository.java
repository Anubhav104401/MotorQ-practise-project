package com.fleetbootcamp.telemetry.repo;

import com.fleetbootcamp.telemetry.dto.TelemetryEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.ZoneOffset;

/** All SQL lives here. Always use ? placeholders — never concatenate input into SQL. */
@Repository
public class TelemetryRepository {
    private final JdbcTemplate jdbc;

    public TelemetryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Example of the style to follow. */
    public boolean vehicleExists(String vin) {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM vehicles WHERE vin = ?", Integer.class, vin);
        return n != null && n > 0;
    }

    /**
     * M2: Idempotent insert of a normalised TelemetryEvent.
     * The unique constraint uq_telemetry_vin_ts on (vin, ts_utc) prevents duplicate rows.
     * Returns true if a new row was inserted, false if it was already present.
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
}
