package com.eyarko.ecom.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class ApplicationMetricsService {
    private final MeterRegistry meterRegistry;
    private final Counter processedOrdersCounter;

    public ApplicationMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.processedOrdersCounter = Counter.builder("app.orders.processed.total")
            .description("Total number of successfully processed orders")
            .register(meterRegistry);
    }

    public void incrementProcessedOrders() {
        processedOrdersCounter.increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopTimer(Timer.Sample sample, String metricName) {
        sample.stop(
            Timer.builder(metricName)
                .description("Execution duration for critical service operation")
                .publishPercentileHistogram()
                .register(meterRegistry)
        );
    }

    public void recordNanos(String metricName, long nanos) {
        Timer.builder(metricName)
            .description("Execution duration for critical service operation")
            .publishPercentileHistogram()
            .register(meterRegistry)
            .record(nanos, TimeUnit.NANOSECONDS);
    }
}
