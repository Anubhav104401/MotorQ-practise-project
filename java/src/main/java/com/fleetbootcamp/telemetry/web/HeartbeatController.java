package com.fleetbootcamp.telemetry.web;

import com.fleetbootcamp.telemetry.dto.HeartbeatRequest;
import com.fleetbootcamp.telemetry.dto.TelemetryEvent;
import com.fleetbootcamp.telemetry.service.IngestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * M1: POST /v1/heartbeats -> validate -> normalise -> publish to Kafka topic telemetry.raw
 */
@RestController
@RequestMapping("/v1")
public class HeartbeatController {

    private final IngestService ingestService;

    public HeartbeatController(IngestService ingestService) {
        this.ingestService = ingestService;
    }

    @PostMapping("/heartbeats")
    public ResponseEntity<Map<String, Object>> ingest(@Valid @RequestBody HeartbeatRequest request) {
        TelemetryEvent event = ingestService.ingest(request);
        Map<String, Object> body = Map.of(
                "status", "accepted",
                "vin", event.vin(),
                "ts_utc", event.tsUtc().toString()
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(body);
    }
}
