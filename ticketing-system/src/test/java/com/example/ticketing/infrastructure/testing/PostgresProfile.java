package com.example.ticketing.infrastructure.testing;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

public class PostgresProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
            "quarkus.devservices.enabled", "false",
            "quarkus.flyway.migrate-at-start", "true");
    }
}
