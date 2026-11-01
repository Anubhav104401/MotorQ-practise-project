package com.fleetbootcamp.telemetry.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Pure rule engine for vehicle overspeed detection.
 * An overspeed alert is triggered when vehicle speed exceeds the threshold (default 100 km/h)
 * for at least minConsecutivePings (default 3) consecutive pings.
 * Exactly one alert row should be created per run, marked by the timestamp of the first ping in the run.
 */
public final class OverspeedRule {

    private OverspeedRule() {
    }

    public record SpeedPing(OffsetDateTime ts, double speedKmph) {
    }

    /**
     * Evaluates consecutive speed pings (provided NEWEST first).
     *
     * @param pingsNewestFirst list of pings ordered newest first
     * @param thresholdKmph speed threshold in km/h (speed must be strictly > threshold)
     * @param minConsecutivePings minimum consecutive pings required to trigger the alert
     * @return Optional containing the start timestamp of the overspeed run, or empty if not currently in a qualifying run
     */
    public static Optional<OffsetDateTime> findRunStart(
            List<SpeedPing> pingsNewestFirst,
            double thresholdKmph,
            int minConsecutivePings
    ) {
        if (pingsNewestFirst == null || pingsNewestFirst.isEmpty() || minConsecutivePings <= 0) {
            return Optional.empty();
        }

        // The latest ping must be above threshold; otherwise, no active overspeed run
        if (pingsNewestFirst.get(0).speedKmph() <= thresholdKmph) {
            return Optional.empty();
        }

        int count = 0;
        OffsetDateTime runStart = null;

        // Traverse backwards from the latest ping as long as pings remain above threshold
        for (SpeedPing ping : pingsNewestFirst) {
            if (ping.speedKmph() > thresholdKmph) {
                count++;
                runStart = ping.ts();
            } else {
                break;
            }
        }

        if (count >= minConsecutivePings) {
            return Optional.ofNullable(runStart);
        }

        return Optional.empty();
    }
}
