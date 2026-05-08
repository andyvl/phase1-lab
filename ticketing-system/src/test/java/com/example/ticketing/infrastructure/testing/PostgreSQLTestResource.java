package com.example.ticketing.infrastructure.testing;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.Map;
import org.testcontainers.containers.PostgreSQLContainer;

public class PostgreSQLTestResource implements QuarkusTestResourceLifecycleManager {
    private PostgreSQLContainer<?> postgres;

    @Override
    public Map<String, String> start() {
        postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("ticketing")
            .withUsername("ticketing")
            .withPassword("ticketing");
        postgres.start();
        return Map.of(
            "quarkus.datasource.db-kind", "postgresql",
            "quarkus.datasource.username", postgres.getUsername(),
            "quarkus.datasource.password", postgres.getPassword(),
            "quarkus.datasource.reactive.url",
            "vertx-reactive:postgresql://%s:%d/%s".formatted(postgres.getHost(), postgres.getMappedPort(5432), postgres.getDatabaseName()),
            "quarkus.datasource.jdbc.url", postgres.getJdbcUrl(),
            "quarkus.flyway.migrate-at-start", "true",
            "quarkus.devservices.enabled", "false");
    }

    @Override
    public void stop() {
        if (postgres != null) {
            postgres.stop();
        }
    }
}
