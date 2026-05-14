package aipasslivesync.backend.dto;

import java.time.Instant;
import java.util.Map;

public record HealthResponse(
        String status,
        Instant startedAt,
        long uptimeSeconds,
        Map<String, Object> queue,
        Map<String, Object> metrics
) {}
