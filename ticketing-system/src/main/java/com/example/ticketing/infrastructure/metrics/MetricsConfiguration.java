package com.example.ticketing.infrastructure.metrics;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import jakarta.inject.Singleton;

/**
 * Configures Micrometer distribution statistics for the HTTP server metrics.
 *
 * Quarkus does not expose percentile configuration via application.properties
 * for the HTTP server binder — a MeterFilter CDI bean is the correct approach.
 *
 * The resulting Prometheus metrics enable accurate server-side p50/p90/p95/p99
 * calculations without relying on client-side estimation:
 *
 *   histogram_quantile(0.95,
 *     sum(rate(http_server_requests_seconds_bucket[15s])) by (le, uri)
 *   )
 */
@Singleton
public class MetricsConfiguration implements MeterFilter {

    @Override
    public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
        if (id.getName().startsWith("http.server.requests")) {
            return DistributionStatisticConfig.builder()
                    .percentiles(0.50, 0.90, 0.95, 0.99)
                    .percentilesHistogram(true)   // emit full histogram buckets for PromQL
                    .build()
                    .merge(config);
        }
        return config;
    }
}
