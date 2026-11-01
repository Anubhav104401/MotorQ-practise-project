package com.fleetbootcamp.telemetry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetbootcamp.telemetry.Topics;
import com.fleetbootcamp.telemetry.dto.HeartbeatRequest;
import com.fleetbootcamp.telemetry.dto.TelemetryEvent;
import com.fleetbootcamp.telemetry.error.ApiErrors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class IngestServiceTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private ObjectMapper objectMapper;
    private Clock fixedClock;
    private IngestService ingestService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        fixedClock = Clock.fixed(Instant.parse("2026-10-28T10:00:00Z"), ZoneOffset.UTC);
        ingestService = new IngestService(kafkaTemplate, objectMapper, fixedClock);
    }

    @Test
    void ingestsValidHeartbeatAndPublishesToKafka() {
        OffsetDateTime ts = OffsetDateTime.parse("2026-10-28T10:00:00Z");
        HeartbeatRequest request = new HeartbeatRequest(
                "1FA6CVNCNCXW927TC",
                ts,
                55.0,
                10000.0,
                "km",
                12.97,
                77.59,
                90,
                50.0,
                true,
                "OEM-A"
        );

        TelemetryEvent event = ingestService.ingest(request);

        assertNotNull(event);
        assertEquals("1FA6CVNCNCXW927TC", event.vin());

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(Topics.RAW), eq("1FA6CVNCNCXW927TC"), jsonCaptor.capture());
        String publishedJson = jsonCaptor.getValue();
        assertEquals(true, publishedJson.contains("\"schema\":\"v1\""));
        assertEquals(true, publishedJson.contains("\"vin\":\"1FA6CVNCNCXW927TC\""));
    }

    @Test
    void rejectsTimestampMoreThan5MinutesInTheFuture() {
        // Clock is at 10:00:00Z, heartbeat at 10:05:01Z (> 5 minutes)
        OffsetDateTime futureTs = OffsetDateTime.parse("2026-10-28T10:05:01Z");
        HeartbeatRequest request = new HeartbeatRequest(
                "1FA6CVNCNCXW927TC",
                futureTs,
                55.0,
                10000.0,
                "km",
                12.97,
                77.59,
                90,
                50.0,
                true,
                "OEM-A"
        );

        ApiErrors.ValidationFailedException ex = assertThrows(
                ApiErrors.ValidationFailedException.class,
                () -> ingestService.ingest(request)
        );

        assertEquals("ts", ex.field());
        verifyNoInteractions(kafkaTemplate);
    }
}
