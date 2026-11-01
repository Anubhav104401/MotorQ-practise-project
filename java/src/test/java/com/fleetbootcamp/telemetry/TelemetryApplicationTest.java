package com.fleetbootcamp.telemetry;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "app.consumer-enabled=false",
        "spring.kafka.bootstrap-servers=localhost:9092",
        "spring.datasource.url=jdbc:postgresql://localhost:5433/fleet"
})
class TelemetryApplicationTest {

    @Test
    void contextLoads() {
        // Verifies the entire Spring application context and all bean wirings load successfully
    }
}
