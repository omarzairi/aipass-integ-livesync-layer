package aipasslivesync.backend.service.workflow;

import aipasslivesync.backend.dto.WorkflowResultDto;
import aipasslivesync.backend.enums.EventType;
import aipasslivesync.backend.enums.WorkflowDecision;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HROnboardingHandler implements WorkflowHandler {

    private static final java.util.Set<String> REQUIRED_FIELDS =
            java.util.Set.of("employee_name", "department", "start_date");

    @Override
    public EventType getSupportedEventType() {
        return EventType.HR_ONBOARDING;
    }

    @Override
    public WorkflowResultDto execute(Map<String, Object> payload) {
        var missing = REQUIRED_FIELDS.stream()
                .filter(f -> !payload.containsKey(f) || payload.get(f) == null)
                .toList();

        if (missing.isEmpty()) {
            return new WorkflowResultDto(
                    "processed", "hr_onboarding",
                    WorkflowDecision.PASS,
                    "All required fields present — onboarding approved for %s".formatted(payload.get("employee_name")));
        }
        return new WorkflowResultDto(
                "processed", "hr_onboarding",
                WorkflowDecision.REVIEW,
                "Missing required fields: %s — onboarding pending review".formatted(String.join(", ", missing)));
    }
}
