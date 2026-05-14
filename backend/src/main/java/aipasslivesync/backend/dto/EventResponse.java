package aipasslivesync.backend.dto;

import aipasslivesync.backend.enums.EventStatus;
import aipasslivesync.backend.enums.EventType;
import java.time.Instant;
import java.util.UUID;

public record EventResponse(
        UUID id,
        EventType eventType,
        String source,
        Object payload,
        EventStatus status,
        Object workflowResult,
        int retryCount,
        Instant createdAt,
        Instant updatedAt
) {}
