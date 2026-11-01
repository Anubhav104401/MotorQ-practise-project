package com.fleetbootcamp.telemetry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetbootcamp.telemetry.Topics;
import com.fleetbootcamp.telemetry.dto.AlertView;
import com.fleetbootcamp.telemetry.dto.TelemetryEvent;
import com.fleetbootcamp.telemetry.dto.TelemetryPage;
import com.fleetbootcamp.telemetry.dto.TelemetryView;
import com.fleetbootcamp.telemetry.repo.TelemetryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service managing telemetry ingestion, query APIs, and overspeed alerts.
 */
@Service
public class TelemetryService {
    private static final Logger log = LoggerFactory.getLogger(TelemetryService.class);

    private final TelemetryRepository telemetryRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final double overspeedKmph;
    private final int overspeedMinPings;

    public TelemetryService(
            TelemetryRepository telemetryRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${app.overspeed-kmph:100}") double overspeedKmph,
            @Value("${app.overspeed-min-pings:3}") int overspeedMinPings
    ) {
        this.telemetryRepository = telemetryRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.overspeedKmph = overspeedKmph;
        this.overspeedMinPings = overspeedMinPings;
    }

    /**
     * M2 & M3: Persist telemetry event and evaluate overspeed alert rules.
     */
    @Transactional
    public boolean processEvent(TelemetryEvent event) {
        boolean inserted = telemetryRepository.insert(event);
        if (inserted) {
            log.debug("Inserted telemetry for vin={} at ts={}", event.vin(), event.tsUtc());
            evaluateOverspeedAlert(event);
        } else {
            log.info("Duplicate telemetry ignored for vin={} at ts={}", event.vin(), event.tsUtc());
        }
        return inserted;
    }

    private void evaluateOverspeedAlert(TelemetryEvent event) {
        // Fast-path: if current ping speed is not above threshold, cannot be active overspeed
        if (event.speedKmph() == null || event.speedKmph() <= overspeedKmph) {
            return;
        }

        // Fetch recent speed pings newest first
        List<OverspeedRule.SpeedPing> recentPings = telemetryRepository.findRecentSpeedPings(event.vin(), overspeedMinPings * 3);
        Optional<OffsetDateTime> runStart = OverspeedRule.findRunStart(recentPings, overspeedKmph, overspeedMinPings);

        if (runStart.isPresent()) {
            OffsetDateTime startedAt = runStart.get();
            Map<String, Object> details = Map.of(
                    "speed_kmph", event.speedKmph(),
                    "limit_kmph", overspeedKmph,
                    "consecutive_pings", overspeedMinPings
            );

            boolean alertCreated = telemetryRepository.insertAlert(event.vin(), "OVERSPEED", startedAt, details);
            if (alertCreated) {
                log.info("🚨 New OVERSPEED alert created for vin={} starting at {}", event.vin(), startedAt);
                publishAlertEvent(event.vin(), "OVERSPEED", startedAt, details);
            }
        }
    }

    private void publishAlertEvent(String vin, String type, OffsetDateTime startedAt, Map<String, Object> details) {
        try {
            Map<String, Object> message = Map.of(
                    "vin", vin,
                    "type", type,
                    "started_at", startedAt.toString(),
                    "details", details
            );
            String json = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(Topics.ALERTS, vin, json);
            log.info("Published alert to {} for vin={}", Topics.ALERTS, vin);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize alert message for vin={}: {}", vin, e.getMessage());
        }
    }

    // --- Query API Delegations (M3) ---

    @Transactional(readOnly = true)
    public Optional<TelemetryView> getLatest(String vin) {
        return telemetryRepository.findLatest(vin);
    }

    @Transactional(readOnly = true)
    public TelemetryPage getRange(String vin, OffsetDateTime from, OffsetDateTime to, int limit, String cursor) {
        return telemetryRepository.findRange(vin, from, to, limit, cursor);
    }

    @Transactional(readOnly = true)
    public List<AlertView> getAlerts(String vin) {
        return telemetryRepository.findAlerts(vin);
    }

    @Transactional(readOnly = true)
    public List<TelemetryView> getFleetLatest(int customerId) {
        return telemetryRepository.findFleetLatest(customerId);
    }
}
