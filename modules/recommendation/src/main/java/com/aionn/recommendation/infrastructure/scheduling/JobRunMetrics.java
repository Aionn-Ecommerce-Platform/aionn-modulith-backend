package com.aionn.recommendation.infrastructure.scheduling;

import com.aionn.recommendation.application.port.out.observability.RecommendationMetricsPort;

import java.time.Duration;

/**
 * Times one job run and reports it, so each scheduler measures the same way and a failure is always
 * recorded rather than only logged.
 *
 * <p>Every job in this package catches its own exceptions to keep one bad run from killing the
 * schedule. That is the right resilience choice and the wrong observability one: a job that fails on
 * every run looks identical to a job that has nothing to do. Routing both outcomes through here means
 * {@code recommendation.scheduler.runtime{job=...,success=false}} is a rate that can be alerted on.
 */
final class JobRunMetrics {

    private final RecommendationMetricsPort metrics;
    private final String jobName;
    private final long startedAtNanos;

    private JobRunMetrics(RecommendationMetricsPort metrics, String jobName, long startedAtNanos) {
        this.metrics = metrics;
        this.jobName = jobName;
        this.startedAtNanos = startedAtNanos;
    }

    static JobRunMetrics start(RecommendationMetricsPort metrics, String jobName) {
        return new JobRunMetrics(metrics, jobName, System.nanoTime());
    }

    void succeeded() {
        record(true);
    }

    void failed() {
        record(false);
    }

    private void record(boolean success) {
        long elapsedMillis = Duration.ofNanos(System.nanoTime() - startedAtNanos).toMillis();
        metrics.recordSchedulerExecution(jobName, elapsedMillis, success);
    }
}
