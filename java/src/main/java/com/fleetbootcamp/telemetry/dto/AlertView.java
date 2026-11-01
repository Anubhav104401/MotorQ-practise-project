package com.fleetbootcamp.telemetry.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.Map;

/** View of an alert row in the alerts table. */
public record AlertView(
        @JsonProperty("id") Long id,
        @JsonProperty("vin") String vin,
        @JsonProperty("type") String type,
        @JsonProperty("started_at") OffsetDateTime startedAt,
        @JsonProperty("details") Map<String, Object> details,
        @JsonProperty("created_at") OffsetDateTime createdAt
) {
}
