package com.fleetbootcamp.telemetry.web;

import com.fleetbootcamp.telemetry.dto.AlertView;
import com.fleetbootcamp.telemetry.dto.TelemetryPage;
import com.fleetbootcamp.telemetry.dto.TelemetryView;
import com.fleetbootcamp.telemetry.error.ApiErrors;
import com.fleetbootcamp.telemetry.service.TelemetryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * M3 Query APIs:
 *   GET /v1/vehicles/{vin}/latest                       404 if no data
 *   GET /v1/vehicles/{vin}/telemetry?from&to&limit&cursor   keyset pagination on ts_utc (next_cursor), NOT OFFSET
 *   GET /v1/vehicles/{vin}/alerts                       newest first
 *   GET /v1/fleets/{customerId}/latest                  ONE SQL query for the whole fleet
 */
@RestController
@RequestMapping("/v1")
public class VehicleController {

    private final TelemetryService telemetryService;

    public VehicleController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @GetMapping("/vehicles/{vin}/latest")
    public ResponseEntity<TelemetryView> getLatest(@PathVariable String vin) {
        TelemetryView latest = telemetryService.getLatest(vin)
                .orElseThrow(() -> new ApiErrors.NotFoundException("No telemetry found for VIN: " + vin));
        return ResponseEntity.ok(latest);
    }

    @GetMapping("/vehicles/{vin}/telemetry")
    public ResponseEntity<TelemetryPage> getRange(
            @PathVariable String vin,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) String cursor
    ) {
        TelemetryPage page = telemetryService.getRange(vin, from, to, limit, cursor);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/vehicles/{vin}/alerts")
    public ResponseEntity<List<AlertView>> getAlerts(@PathVariable String vin) {
        List<AlertView> alerts = telemetryService.getAlerts(vin);
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/fleets/{customerId}/latest")
    public ResponseEntity<List<TelemetryView>> getFleetLatest(@PathVariable int customerId) {
        List<TelemetryView> fleet = telemetryService.getFleetLatest(customerId);
        return ResponseEntity.ok(fleet);
    }
}
