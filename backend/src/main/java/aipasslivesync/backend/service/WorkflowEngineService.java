package aipasslivesync.backend.service;

import aipasslivesync.backend.dto.WorkflowResultDto;
import aipasslivesync.backend.enums.EventType;
import aipasslivesync.backend.service.workflow.WorkflowHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class WorkflowEngineService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEngineService.class);
    private final Map<EventType, WorkflowHandler> handlers = new EnumMap<>(EventType.class);

    public WorkflowEngineService(List<WorkflowHandler> handlerList) {
        for (WorkflowHandler h : handlerList) {
            handlers.put(h.getSupportedEventType(), h);
            log.info("Registered workflow handler: {} -> {}", h.getSupportedEventType(), h.getClass().getSimpleName());
        }
    }

    public WorkflowResultDto execute(EventType eventType, Map<String, Object> payload) {
        WorkflowHandler handler = handlers.get(eventType);
        if (handler == null) {
            throw new IllegalArgumentException("No workflow handler registered for event type: " + eventType);
        }
        return handler.execute(payload);
    }

    public boolean hasHandler(EventType eventType) {
        return handlers.containsKey(eventType);
    }
}
