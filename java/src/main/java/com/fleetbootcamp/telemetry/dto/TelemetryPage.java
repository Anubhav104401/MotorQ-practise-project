package com.fleetbootcamp.telemetry.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Keyset paginated telemetry response. */
public record TelemetryPage(
        @JsonProperty("data") List<TelemetryView> data,
        @JsonProperty("next_cursor") String nextCursor
) {
}
