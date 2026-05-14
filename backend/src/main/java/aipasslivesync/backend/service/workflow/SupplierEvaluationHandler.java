package aipasslivesync.backend.service.workflow;

import aipasslivesync.backend.dto.WorkflowResultDto;
import aipasslivesync.backend.enums.EventType;
import aipasslivesync.backend.enums.WorkflowDecision;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SupplierEvaluationHandler implements WorkflowHandler {

    @Override
    public EventType getSupportedEventType() {
        return EventType.SUPPLIER_UPDATED;
    }

    @Override
    public WorkflowResultDto execute(Map<String, Object> payload) {
        String status = (String) payload.getOrDefault("status", "unknown");
        double rating = extractRating(payload);

        if ("active".equalsIgnoreCase(status) && rating >= 3.0) {
            return new WorkflowResultDto(
                    "processed", "supplier_evaluation",
                    WorkflowDecision.PASS,
                    "Supplier is active with rating %.1f — approved".formatted(rating));
        }
        if (rating < 2.0) {
            return new WorkflowResultDto(
                    "processed", "supplier_evaluation",
                    WorkflowDecision.REJECT,
                    "Supplier rating %.1f is below minimum — rejected".formatted(rating));
        }
        return new WorkflowResultDto(
                "processed", "supplier_evaluation",
                WorkflowDecision.REVIEW,
                "Supplier requires manual evaluation (status=%s, rating=%.1f)".formatted(status, rating));
    }

    private double extractRating(Map<String, Object> payload) {
        Object raw = payload.get("rating");
        if (raw instanceof Number n) return n.doubleValue();
        if (raw instanceof String s) return Double.parseDouble(s);
        return 0.0;
    }
}
