package com.fleetbootcamp.telemetry.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.OffsetDateTime;

/**
 * What a device (or OEM feed) sends to POST /v1/heartbeats.
 *
 * M1 Bean Validation (jakarta.validation.constraints.*):
 *   - vin: exactly 17 chars, A-Z and 0-9, never I, O or Q (@Pattern)
 *   - ts: required; OffsetDateTime already rejects timestamps without a timezone. "Not in the future" -> done in IngestService.
 *   - speed_kmph 0..300, lat -90..90, lon -180..180, odometer >= 0, heading 0..359, soc_pct 0..100
 *   - odometer_unit only "km" or "mi" (null defaults to km)
 */
public record HeartbeatRequest(
        @NotNull(message = "vin is required")
        @Pattern(regexp = "^[A-HJ-NPR-Za-hj-npr-z0-9]{17}$",
                 message = "vin must be 17 characters, A-Z and 0-9 without I, O, or Q")
        @JsonProperty("vin")
        String vin,

        @NotNull(message = "ts is required")
        @JsonProperty("ts")
        OffsetDateTime ts,

        @NotNull(message = "speed_kmph is required")
        @DecimalMin(value = "0.0", message = "speed_kmph must be at least 0")
        @DecimalMax(value = "300.0", message = "speed_kmph must be at most 300")
        @JsonProperty("speed_kmph")
        Double speedKmph,

        @NotNull(message = "odometer is required")
        @DecimalMin(value = "0.0", message = "odometer must be at least 0")
        @JsonProperty("odometer")
        Double odometer,

        @Pattern(regexp = "^(km|mi)$", message = "odometer_unit must be km or mi")
        @JsonProperty("odometer_unit")
        String odometerUnit,

        @NotNull(message = "lat is required")
        @DecimalMin(value = "-90.0", message = "lat must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "lat must be between -90 and 90")
        @JsonProperty("lat")
        Double lat,

        @NotNull(message = "lon is required")
        @DecimalMin(value = "-180.0", message = "lon must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "lon must be between -180 and 180")
        @JsonProperty("lon")
        Double lon,

        @Min(value = 0, message = "heading must be between 0 and 359")
        @Max(value = 359, message = "heading must be between 0 and 359")
        @JsonProperty("heading")
        Integer heading,

        @DecimalMin(value = "0.0", message = "soc_pct must be between 0 and 100")
        @DecimalMax(value = "100.0", message = "soc_pct must be between 0 and 100")
        @JsonProperty("soc_pct")
        Double socPct,

        @JsonProperty("ignition")
        Boolean ignition,

        @NotNull(message = "source_oem is required")
        @JsonProperty("source_oem")
        String sourceOem
) {
}