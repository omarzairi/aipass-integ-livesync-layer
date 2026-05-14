package aipasslivesync.backend.service.workflow;

import aipasslivesync.backend.dto.WorkflowResultDto;
import aipasslivesync.backend.enums.EventType;
import aipasslivesync.backend.enums.WorkflowDecision;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class InvoiceReviewHandler implements WorkflowHandler {

    private static final double THRESHOLD = 5000.0;

    @Override
    public EventType getSupportedEventType() {
        return EventType.INVOICE_UPLOADED;
    }

    @Override
    public WorkflowResultDto execute(Map<String, Object> payload) {
        double amount = extractAmount(payload);

        if (amount <= THRESHOLD) {
            return new WorkflowResultDto(
                    "processed", "invoice_review",
                    WorkflowDecision.PASS,
                    "Amount %.2f is within threshold (%.2f)".formatted(amount, THRESHOLD));
        }
        return new WorkflowResultDto(
                "processed", "invoice_review",
                WorkflowDecision.REVIEW,
                "Amount %.2f exceeds threshold (%.2f) — manual review required".formatted(amount, THRESHOLD));
    }

    private double extractAmount(Map<String, Object> payload) {
        Object raw = payload.get("amount");
        if (raw instanceof Number n) return n.doubleValue();
        if (raw instanceof String s) return Double.parseDouble(s);
        return 0.0;
    }
}
