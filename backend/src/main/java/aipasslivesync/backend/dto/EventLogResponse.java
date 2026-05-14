package aipasslivesync.backend.dto;

import aipasslivesync.backend.enums.LogLevel;
import java.time.Instant;
import java.util.UUID;

public record EventLogResponse(
        UUID id,
        UUID eventId,
        LogLevel level,
        String message,
        Instant timestamp
) {}
