package com.fleetbootcamp.telemetry.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fleetbootcamp.telemetry.Topics;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * M2: DefaultErrorHandler configuration for Kafka listeners.
 *   - unparseable / wrong-schema messages: no retry, publish to telemetry.dlq (DeadLetterPublishingRecoverer)
 *   - transient errors (e.g. DB briefly down): 3 retries with a back-off, then DLQ
 */
@Configuration
public class KafkaConfig {

    @Bean
    public DefaultErrorHandler defaultErrorHandler(KafkaOperations<Object, Object> kafkaOperations) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaOperations,
                (record, exception) -> new TopicPartition(Topics.DLQ, -1)
        );

        FixedBackOff backOff = new FixedBackOff(1000L, 3L);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        errorHandler.addNotRetryableExceptions(
                InvalidTelemetryMessageException.class,
                JsonProcessingException.class
        );

        return errorHandler;
    }
}
