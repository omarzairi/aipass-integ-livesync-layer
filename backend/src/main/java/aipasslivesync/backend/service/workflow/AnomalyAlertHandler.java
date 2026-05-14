package aipasslivesync.backend.service.workflow;

import aipasslivesync.backend.dto.WorkflowResultDto;
import aipasslivesync.backend.enums.EventType;
import aipasslivesync.backend.enums.WorkflowDecision;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AnomalyAlertHandler implements WorkflowHandler {

    @Override
    public EventType getSupportedEventType() {
        return EventType.ANOMALY_ALERT;
    }

    @Override
    public WorkflowResultDto execute(Map<String, Object> payload) {
        String severity = ((String) payload.getOrDefault("severity", "low")).toLowerCase();
        double confidence = extractConfidence(payload);

        if ("critical".equals(severity) || confidence >= 0.9) {
            return new WorkflowResultDto(
                    "processed", "anomaly_alert",
                    WorkflowDecision.FLAG,
                    "Critical anomaly detected (severity=%s, confidence=%.2f) — flagged for immediate action".formatted(severity, confidence));
        }
        if ("high".equals(severity) || confidence >= 0.7) {
            return new WorkflowResultDto(
                    "processed", "anomaly_alert",
                    WorkflowDecision.REVIEW,
                    "High-severity anomaly (severity=%s, confidence=%.2f) — queued for review".formatted(severity, confidence));
        }
        return new WorkflowResultDto(
                "processed", "anomaly_alert",
                WorkflowDecision.PASS,
                "Low-severity anomaly (severity=%s, confidence=%.2f) — logged, no action needed".formatted(severity, confidence));
    }

    private double extractConfidence(Map<String, Object> payload) {
        Object raw = payload.get("confidence");
        if (raw instanceof Number n) return n.doubleValue();
        if (raw instanceof String s) return Double.parseDouble(s);
        return 0.0;
    }
}
