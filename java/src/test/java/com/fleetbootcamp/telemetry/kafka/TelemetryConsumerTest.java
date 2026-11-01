package com.fleetbootcamp.telemetry.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetbootcamp.telemetry.dto.TelemetryEvent;
import com.fleetbootcamp.telemetry.service.TelemetryService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TelemetryConsumerTest {

    @Mock
    private TelemetryService telemetryService;

    private ObjectMapper objectMapper;
    private TelemetryConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        consumer = new TelemetryConsumer(objectMapper, telemetryService);
    }

    @Test
    void consumesValidTelemetryEventSuccessfully() {
        String json = """
                {
                    "schema": "v1",
                    "vin": "1FA6CVNCNCXW927TC",
                    "ts_utc": "2026-10-28T10:00:00Z",
                    "speed_kmph": 60.5,
                    "odometer_km": 15000.0,
                    "lat": 12.97,
                    "lon": 77.59,
                    "heading": 90,
                    "soc_pct": 80.0,
                    "ignition": true,
                    "source_oem": "OEM-A"
                }
                """;
        ConsumerRecord<String, String> record = new ConsumerRecord<>("telemetry.raw", 0, 10L, "1FA6CVNCNCXW927TC", json);

        consumer.onMessage(record);

        ArgumentCaptor<TelemetryEvent> captor = ArgumentCaptor.forClass(TelemetryEvent.class);
        verify(telemetryService).processEvent(captor.capture());
        TelemetryEvent event = captor.getValue();
        assertEquals("1FA6CVNCNCXW927TC", event.vin());
        assertEquals(Instant.parse("2026-10-28T10:00:00Z"), event.tsUtc());
        assertEquals(60.5, event.speedKmph());
    }

    @Test
    void unparseableJsonThrowsInvalidTelemetryMessageException() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("telemetry.raw", 0, 11L, "KEY", "{not a valid json");

        assertThrows(InvalidTelemetryMessageException.class, () -> consumer.onMessage(record));
        verifyNoInteractions(telemetryService);
    }

    @Test
    void wrongSchemaThrowsInvalidTelemetryMessageException() {
        String json = """
                {
                    "schema": "v2",
                    "vin": "1FA6CVNCNCXW927TC",
                    "ts_utc": "2026-10-28T10:00:00Z",
                    "speed_kmph": 60.5,
                    "odometer_km": 15000.0,
                    "lat": 12.97,
                    "lon": 77.59,
                    "source_oem": "OEM-A"
                }
                """;
        ConsumerRecord<String, String> record = new ConsumerRecord<>("telemetry.raw", 0, 12L, "1FA6CVNCNCXW927TC", json);

        assertThrows(InvalidTelemetryMessageException.class, () -> consumer.onMessage(record));
        verifyNoInteractions(telemetryService);
    }

    @Test
    void missingRequiredFieldsThrowsInvalidTelemetryMessageException() {
        String json = """
                {
                    "schema": "v1",
                    "vin": "1FA6CVNCNCXW927TC"
                }
                """;
        ConsumerRecord<String, String> record = new ConsumerRecord<>("telemetry.raw", 0, 13L, "1FA6CVNCNCXW927TC", json);

        assertThrows(InvalidTelemetryMessageException.class, () -> consumer.onMessage(record));
        verifyNoInteractions(telemetryService);
    }
}
