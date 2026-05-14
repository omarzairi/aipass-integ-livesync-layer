package aipasslivesync.backend.dto;

import aipasslivesync.backend.enums.WorkflowDecision;

public record WorkflowResultDto(
        String status,
        String workflow,
        WorkflowDecision decision,
        String reason
) {}
