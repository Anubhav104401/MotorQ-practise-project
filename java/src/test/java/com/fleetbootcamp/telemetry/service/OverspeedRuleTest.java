package com.fleetbootcamp.telemetry.service;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverspeedRuleTest {

    private static final double LIMIT = 100.0;
    private static final int MIN_PINGS = 3;
    private static final OffsetDateTime T0 = OffsetDateTime.of(2026, 10, 28, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime T1 = T0.plusSeconds(5);
    private static final OffsetDateTime T2 = T0.plusSeconds(10);
    private static final OffsetDateTime T3 = T0.plusSeconds(15);
    private static final OffsetDateTime T4 = T0.plusSeconds(20);
    private static final OffsetDateTime T5 = T0.plusSeconds(25);

    @Test
    void exactly100KmphDoesNotTriggerAlert() {
        // Strict inequality (> 100): 100.0 is not an overspeed
        List<OverspeedRule.SpeedPing> pings = List.of(
                new OverspeedRule.SpeedPing(T2, 100.0),
                new OverspeedRule.SpeedPing(T1, 100.0),
                new OverspeedRule.SpeedPing(T0, 100.0)
        );

        Optional<OffsetDateTime> start = OverspeedRule.findRunStart(pings, LIMIT, MIN_PINGS);
        assertTrue(start.isEmpty(), "Exactly 100 km/h must not trigger an overspeed alert");
    }

    @Test
    void runOfTwoDoesNotTriggerAlert() {
        // Only 2 consecutive pings above 100 km/h, min is 3
        List<OverspeedRule.SpeedPing> pings = List.of(
                new OverspeedRule.SpeedPing(T2, 105.0),
                new OverspeedRule.SpeedPing(T1, 110.0),
                new OverspeedRule.SpeedPing(T0, 60.0)
        );

        Optional<OffsetDateTime> start = OverspeedRule.findRunStart(pings, LIMIT, MIN_PINGS);
        assertTrue(start.isEmpty(), "Run of only 2 pings must not trigger an alert");
    }

    @Test
    void runBrokenBySlowPingDoesNotTriggerAlert() {
        // Latest 2 pings are >100, but previous ping broke the run with 60 km/h
        List<OverspeedRule.SpeedPing> pings = List.of(
                new OverspeedRule.SpeedPing(T4, 115.0),
                new OverspeedRule.SpeedPing(T3, 112.0),
                new OverspeedRule.SpeedPing(T2, 60.0),
                new OverspeedRule.SpeedPing(T1, 120.0),
                new OverspeedRule.SpeedPing(T0, 125.0)
        );

        Optional<OffsetDateTime> start = OverspeedRule.findRunStart(pings, LIMIT, MIN_PINGS);
        assertTrue(start.isEmpty(), "Run broken by a slow ping (current streak < 3) must not trigger an alert");
    }

    @Test
    void runOfExactlyThreeTriggersAlertWithFirstPingTimestamp() {
        // Ping 1 (T1: 105), Ping 2 (T2: 112), Ping 3 (T3: 118)
        List<OverspeedRule.SpeedPing> pings = List.of(
                new OverspeedRule.SpeedPing(T3, 118.0),
                new OverspeedRule.SpeedPing(T2, 112.0),
                new OverspeedRule.SpeedPing(T1, 105.0),
                new OverspeedRule.SpeedPing(T0, 60.0)
        );

        Optional<OffsetDateTime> start = OverspeedRule.findRunStart(pings, LIMIT, MIN_PINGS);
        assertTrue(start.isPresent());
        assertEquals(T1, start.get(), "Alert start timestamp must be the first ping of the run (T1)");
    }

    @Test
    void runThatKeepsGrowingMaintainsSameRunStartTimestamp() {
        // 4th ping (T4: 121) and 5th ping (T5: 125) continue the same run
        List<OverspeedRule.SpeedPing> pings4 = List.of(
                new OverspeedRule.SpeedPing(T4, 121.0),
                new OverspeedRule.SpeedPing(T3, 118.0),
                new OverspeedRule.SpeedPing(T2, 112.0),
                new OverspeedRule.SpeedPing(T1, 105.0),
                new OverspeedRule.SpeedPing(T0, 60.0)
        );
        Optional<OffsetDateTime> start4 = OverspeedRule.findRunStart(pings4, LIMIT, MIN_PINGS);
        assertTrue(start4.isPresent());
        assertEquals(T1, start4.get(), "4th ping must still have run start at T1");

        List<OverspeedRule.SpeedPing> pings5 = List.of(
                new OverspeedRule.SpeedPing(T5, 125.0),
                new OverspeedRule.SpeedPing(T4, 121.0),
                new OverspeedRule.SpeedPing(T3, 118.0),
                new OverspeedRule.SpeedPing(T2, 112.0),
                new OverspeedRule.SpeedPing(T1, 105.0),
                new OverspeedRule.SpeedPing(T0, 60.0)
        );
        Optional<OffsetDateTime> start5 = OverspeedRule.findRunStart(pings5, LIMIT, MIN_PINGS);
        assertTrue(start5.isPresent());
        assertEquals(T1, start5.get(), "5th ping must still maintain the same run start at T1");
    }

    @Test
    void returnsEmptyOnEmptyOrNullPings() {
        assertTrue(OverspeedRule.findRunStart(null, LIMIT, MIN_PINGS).isEmpty());
        assertTrue(OverspeedRule.findRunStart(Collections.emptyList(), LIMIT, MIN_PINGS).isEmpty());
    }
}
