package aipasslivesync.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record WebhookRequest(
        @NotBlank(message = "event_type is required")
        String event_type,

        @NotBlank(message = "source is required")
        String source,

        @NotNull(message = "payload is required")
        Map<String, Object> payload
) {}
