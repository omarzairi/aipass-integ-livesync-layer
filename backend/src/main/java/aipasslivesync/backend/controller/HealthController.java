package aipasslivesync.backend.controller;

import aipasslivesync.backend.dto.HealthResponse;
import aipasslivesync.backend.enums.EventStatus;
import aipasslivesync.backend.service.EventService;
import aipasslivesync.backend.service.MetricsService;
import aipasslivesync.backend.service.QueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Health", description = "Service health and monitoring")
public class HealthController {

    private static final Instant STARTED_AT = Instant.now();

    private final QueueService queueService;
    private final MetricsService metricsService;
    private final EventService eventService;

    public HealthController(QueueService queueService,
                            MetricsService metricsService,
                            EventService eventService) {
        this.queueService = queueService;
        this.metricsService = metricsService;
        this.eventService = eventService;
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Returns service status, queue depth, uptime, and basic metrics")
    public HealthResponse health() {
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();

        Map<String, Object> queue = new LinkedHashMap<>();
        queue.put("depth", queueService.getQueueDepth());
        queue.put("redis_available", queueService.isRedisAvailable());

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("events_received", metricsService.getReceivedCount());
        metrics.put("events_processed", metricsService.getProcessedCount());
        metrics.put("events_failed", metricsService.getFailedCount());
        metrics.put("events_pending", eventService.countByStatus(EventStatus.PENDING));
        metrics.put("events_dead_letter", eventService.countByStatus(EventStatus.DEAD_LETTER));

        return new HealthResponse("UP", STARTED_AT, uptimeMs / 1000, queue, metrics);
    }
}
