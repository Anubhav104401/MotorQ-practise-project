package com.fleetbootcamp.telemetry.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetbootcamp.telemetry.Topics;
import com.fleetbootcamp.telemetry.dto.TelemetryEvent;
import com.fleetbootcamp.telemetry.service.TelemetryService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * M2: telemetry.raw -> Postgres.
 * application.yml sets enable-auto-commit=false and ack-mode=record: the offset is committed only after this
 * method returns without an exception.
 */
@Component
@ConditionalOnProperty(name = "app.consumer-enabled", havingValue = "true", matchIfMissing = true)
public class TelemetryConsumer {
    private static final Logger log = LoggerFactory.getLogger(TelemetryConsumer.class);

    private final ObjectMapper objectMapper;
    private final TelemetryService telemetryService;

    public TelemetryConsumer(ObjectMapper objectMapper, TelemetryService telemetryService) {
        this.objectMapper = objectMapper;
        this.telemetryService = telemetryService;
    }

    @KafkaListener(topics = Topics.RAW)
    public void onMessage(ConsumerRecord<String, String> record) {
        log.info("partition={} offset={} key={} value={}", record.partition(), record.offset(), record.key(), record.value());

        if (record.value() == null || record.value().isBlank()) {
            throw new InvalidTelemetryMessageException("Empty message value at partition=" + record.partition() + " offset=" + record.offset());
        }

        TelemetryEvent event;
        try {
            event = objectMapper.readValue(record.value(), TelemetryEvent.class);
        } catch (JsonProcessingException e) {
            log.warn("Malformed JSON in telemetry record at partition={} offset={}: {}", record.partition(), record.offset(), e.getMessage());
            throw new InvalidTelemetryMessageException("Malformed JSON: " + e.getMessage(), e);
        }

        if (event == null || !TelemetryEvent.SCHEMA_V1.equals(event.schema())) {
            log.warn("Invalid schema '{}' at partition={} offset={}", event == null ? "null" : event.schema(), record.partition(), record.offset());
            throw new InvalidTelemetryMessageException("Invalid or missing schema: " + (event == null ? "null" : event.schema()));
        }

        if (event.vin() == null || event.vin().isBlank()
                || event.tsUtc() == null
                || event.speedKmph() == null
                || event.odometerKm() == null
                || event.lat() == null
                || event.lon() == null
                || event.sourceOem() == null) {
            log.warn("Missing required fields in event at partition={} offset={}", record.partition(), record.offset());
            throw new InvalidTelemetryMessageException("Missing required fields in TelemetryEvent");
        }

        telemetryService.processEvent(event);
    }
}
