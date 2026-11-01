package com.fleetbootcamp.telemetry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetbootcamp.telemetry.Topics;
import com.fleetbootcamp.telemetry.dto.HeartbeatRequest;
import com.fleetbootcamp.telemetry.dto.TelemetryEvent;
import com.fleetbootcamp.telemetry.error.ApiErrors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class IngestService {
    private static final Logger log = LoggerFactory.getLogger(IngestService.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public IngestService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this(kafkaTemplate, objectMapper, Clock.systemUTC());
    }

    public IngestService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper, Clock clock) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public TelemetryEvent ingest(HeartbeatRequest request) {
        Instant maxFuture = clock.instant().plus(Duration.ofMinutes(5));
        if (request.ts().toInstant().isAfter(maxFuture)) {
            throw new ApiErrors.ValidationFailedException("ts", "timestamp cannot be more than 5 minutes in the future");
        }

        TelemetryEvent event = Normalizer.normalize(request);
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(Topics.RAW, event.vin(), json);
            log.info("Published telemetry event to {} with key={}", Topics.RAW, event.vin());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize TelemetryEvent to JSON", e);
        }

        return event;
    }
}
