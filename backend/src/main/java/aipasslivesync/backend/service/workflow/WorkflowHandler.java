package aipasslivesync.backend.service.workflow;

import aipasslivesync.backend.dto.WorkflowResultDto;
import aipasslivesync.backend.enums.EventType;

import java.util.Map;

public interface WorkflowHandler {

    EventType getSupportedEventType();

    WorkflowResultDto execute(Map<String, Object> payload);
}
