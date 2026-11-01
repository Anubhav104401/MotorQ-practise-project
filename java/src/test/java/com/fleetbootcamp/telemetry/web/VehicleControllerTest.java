package com.fleetbootcamp.telemetry.web;

import com.fleetbootcamp.telemetry.dto.AlertView;
import com.fleetbootcamp.telemetry.dto.TelemetryPage;
import com.fleetbootcamp.telemetry.dto.TelemetryView;
import com.fleetbootcamp.telemetry.service.TelemetryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VehicleController.class)
class VehicleControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    TelemetryService telemetryService;

    @Test
    void getLatestReturns200WhenFound() throws Exception {
        OffsetDateTime ts = OffsetDateTime.parse("2026-10-28T10:00:00Z");
        TelemetryView view = new TelemetryView(
                "1FA6CVNCNCXW927TC",
                ts,
                55.0,
                12000.0,
                12.97,
                77.59,
                90,
                null,
                true,
                "OEM-A"
        );
        when(telemetryService.getLatest("1FA6CVNCNCXW927TC")).thenReturn(Optional.of(view));

        mvc.perform(get("/v1/vehicles/1FA6CVNCNCXW927TC/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vin").value("1FA6CVNCNCXW927TC"))
                .andExpect(jsonPath("$.speed_kmph").value(55.0))
                .andExpect(jsonPath("$.odometer_km").value(12000.0));
    }

    @Test
    void getLatestReturns404WhenNotFound() throws Exception {
        when(telemetryService.getLatest("1FA6CVNCNCXW927TC")).thenReturn(Optional.empty());

        mvc.perform(get("/v1/vehicles/1FA6CVNCNCXW927TC/latest"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getTelemetryReturnsKeysetPage() throws Exception {
        OffsetDateTime ts = OffsetDateTime.parse("2026-10-28T10:00:00Z");
        TelemetryView view = new TelemetryView(
                "1FA6CVNCNCXW927TC",
                ts,
                55.0,
                12000.0,
                12.97,
                77.59,
                90,
                null,
                true,
                "OEM-A"
        );
        TelemetryPage page = new TelemetryPage(List.of(view), "2026-10-28T10:00:00Z");

        when(telemetryService.getRange(eq("1FA6CVNCNCXW927TC"), any(), any(), anyInt(), any()))
                .thenReturn(page);

        mvc.perform(get("/v1/vehicles/1FA6CVNCNCXW927TC/telemetry?limit=50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].vin").value("1FA6CVNCNCXW927TC"))
                .andExpect(jsonPath("$.next_cursor").value("2026-10-28T10:00:00Z"));
    }

    @Test
    void getAlertsReturnsNewestFirst() throws Exception {
        OffsetDateTime start = OffsetDateTime.parse("2026-10-28T10:00:00Z");
        OffsetDateTime created = OffsetDateTime.parse("2026-10-28T10:00:15Z");
        AlertView alert = new AlertView(
                1L,
                "1FA6CVNCNCXW927TC",
                "OVERSPEED",
                start,
                Map.of("speed_kmph", 115.0),
                created
        );

        when(telemetryService.getAlerts("1FA6CVNCNCXW927TC")).thenReturn(List.of(alert));

        mvc.perform(get("/v1/vehicles/1FA6CVNCNCXW927TC/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("OVERSPEED"))
                .andExpect(jsonPath("$[0].vin").value("1FA6CVNCNCXW927TC"));
    }

    @Test
    void getFleetLatestReturnsOneQueryResults() throws Exception {
        OffsetDateTime ts = OffsetDateTime.parse("2026-10-28T10:00:00Z");
        TelemetryView v1 = new TelemetryView("1FA6CVNCNCXW927TC", ts, 55.0, 12000.0, 12.97, 77.59, 90, null, true, "OEM-A");
        TelemetryView v2 = new TelemetryView("5YJ3E1EB8KF314251", ts, 70.0, 8000.0, 12.98, 77.60, 180, 80.0, true, "OEM-D");

        when(telemetryService.getFleetLatest(1)).thenReturn(List.of(v1, v2));

        mvc.perform(get("/v1/fleets/1/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].vin").value("1FA6CVNCNCXW927TC"))
                .andExpect(jsonPath("$[1].vin").value("5YJ3E1EB8KF314251"));
    }
}
