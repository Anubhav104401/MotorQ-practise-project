package com.fleetbootcamp.telemetry.kafka;

/**
 * Thrown when a Kafka message has invalid JSON, wrong schema, or missing required fields.
 * Marked as non-retryable in KafkaConfig so it routes immediately to DLQ without blocking the partition.
 */
public class InvalidTelemetryMessageException extends RuntimeException {
    public InvalidTelemetryMessageException(String message) {
        super(message);
    }

    public InvalidTelemetryMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
