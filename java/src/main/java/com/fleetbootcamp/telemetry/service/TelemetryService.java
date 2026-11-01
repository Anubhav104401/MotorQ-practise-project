package com.fleetbootcamp.telemetry.service;

import com.fleetbootcamp.telemetry.dto.TelemetryEvent;
import com.fleetbootcamp.telemetry.repo.TelemetryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service executing transactional writes to Postgres.
 */
@Service
public class TelemetryService {
    private static final Logger log = LoggerFactory.getLogger(TelemetryService.class);

    private final TelemetryRepository telemetryRepository;

    public TelemetryService(TelemetryRepository telemetryRepository) {
        this.telemetryRepository = telemetryRepository;
    }

    @Transactional
    public boolean processEvent(TelemetryEvent event) {
        boolean inserted = telemetryRepository.insert(event);
        if (inserted) {
            log.debug("Inserted telemetry for vin={} at ts={}", event.vin(), event.tsUtc());
        } else {
            log.info("Duplicate telemetry ignored for vin={} at ts={}", event.vin(), event.tsUtc());
        }
        return inserted;
    }
}
