package com.fleetbootcamp.telemetry.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetbootcamp.telemetry.dto.HeartbeatRequest;
import com.fleetbootcamp.telemetry.dto.TelemetryEvent;
import com.fleetbootcamp.telemetry.error.ApiErrors;
import com.fleetbootcamp.telemetry.service.IngestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HeartbeatController.class)
class HeartbeatControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    IngestService ingestService;

    private Map<String, Object> validPayload() {
        Map<String, Object> map = new HashMap<>();
        map.put("vin", "1FA6CVNCNCXW927TC");
        map.put("ts", "2026-10-28T10:00:00Z");
        map.put("speed_kmph", 54.2);
        map.put("odometer", 10234.5);
        map.put("odometer_unit", "km");
        map.put("lat", 12.97);
        map.put("lon", 77.59);
        map.put("heading", 90);
        map.put("soc_pct", null);
        map.put("ignition", true);
        map.put("source_oem", "OEM-A");
        return map;
    }

    @Test
    void validHeartbeatReturns202() throws Exception {
        TelemetryEvent event = new TelemetryEvent(
                TelemetryEvent.SCHEMA_V1,
                "1FA6CVNCNCXW927TC",
                Instant.parse("2026-10-28T10:00:00Z"),
                54.2,
                10234.5,
                12.97,
                77.59,
                90,
                null,
                true,
                "OEM-A"
        );
        when(ingestService.ingest(any(HeartbeatRequest.class))).thenReturn(event);

        mvc.perform(post("/v1/heartbeats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("accepted"))
                .andExpect(jsonPath("$.vin").value("1FA6CVNCNCXW927TC"))
                .andExpect(jsonPath("$.ts_utc").value("2026-10-28T10:00:00Z"));
    }

    @Test
    void shortVinReturns400() throws Exception {
        Map<String, Object> payload = validPayload();
        payload.put("vin", "TOO-SHORT");

        mvc.perform(post("/v1/heartbeats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("vin"));
    }

    @Test
    void vinWithForbiddenLettersReturns400() throws Exception {
        Map<String, Object> payload = validPayload();
        payload.put("vin", "1HGCM82633A0O4352"); // contains 'O'

        mvc.perform(post("/v1/heartbeats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("vin"));
    }

    @Test
    void missingVinReturns400() throws Exception {
        Map<String, Object> payload = validPayload();
        payload.remove("vin");

        mvc.perform(post("/v1/heartbeats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("vin"));
    }

    @Test
    void negativeSpeedReturns400() throws Exception {
        Map<String, Object> payload = validPayload();
        payload.put("speed_kmph", -5.0);

        mvc.perform(post("/v1/heartbeats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("speed_kmph"));
    }

    @Test
    void speedOver300Returns400() throws Exception {
        Map<String, Object> payload = validPayload();
        payload.put("speed_kmph", 305.0);

        mvc.perform(post("/v1/heartbeats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("speed_kmph"));
    }

    @Test
    void latOutOfRangeReturns400() throws Exception {
        Map<String, Object> payload = validPayload();
        payload.put("lat", 123.0);

        mvc.perform(post("/v1/heartbeats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("lat"));
    }

    @Test
    void unknownOdometerUnitReturns400() throws Exception {
        Map<String, Object> payload = validPayload();
        payload.put("odometer_unit", "furlong");

        mvc.perform(post("/v1/heartbeats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("odometer_unit"));
    }

    @Test
    void malformedTimestampFormatReturns400() throws Exception {
        Map<String, Object> payload = validPayload();
        payload.put("ts", "27/10/2026 10:00");

        mvc.perform(post("/v1/heartbeats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("ts"));
    }

    @Test
    void futureTimestampReturns400() throws Exception {
        when(ingestService.ingest(any(HeartbeatRequest.class)))
                .thenThrow(new ApiErrors.ValidationFailedException("ts", "timestamp cannot be more than 5 minutes in the future"));

        mvc.perform(post("/v1/heartbeats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("ts"));
    }
}
