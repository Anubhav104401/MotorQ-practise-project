package com.fleetbootcamp.telemetry.service;

import com.fleetbootcamp.telemetry.dto.HeartbeatRequest;
import com.fleetbootcamp.telemetry.dto.TelemetryEvent;

import java.time.Instant;
import java.util.Locale;

/** Device heartbeat -> normalised TelemetryEvent (metric units, UTC). Keep it pure so it's easy to unit-test. */
public final class Normalizer {
    public static final double KM_PER_MILE = 1.609344;

    private Normalizer() {
    }

    public static TelemetryEvent normalize(HeartbeatRequest hb) {
        String vin = hb.vin().toUpperCase(Locale.ROOT);
        Instant tsUtc = hb.ts().toInstant();

        Double odometerKm = hb.odometer();
        if (hb.odometerUnit() != null) {
            String unit = hb.odometerUnit().trim().toLowerCase(Locale.ROOT);
            if ("mi".equals(unit)) {
                if (odometerKm != null) {
                    odometerKm = odometerKm * KM_PER_MILE;
                }
            } else if (!"km".equals(unit)) {
                throw new IllegalArgumentException("Unknown odometer unit: " + hb.odometerUnit());
            }
        }

        return new TelemetryEvent(
                TelemetryEvent.SCHEMA_V1,
                vin,
                tsUtc,
                hb.speedKmph(),
                odometerKm,
                hb.lat(),
                hb.lon(),
                hb.heading(),
                hb.socPct(),
                hb.ignition() != null ? hb.ignition() : Boolean.TRUE,
                hb.sourceOem()
        );
    }
}
