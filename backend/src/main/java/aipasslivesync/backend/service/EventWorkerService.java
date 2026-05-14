package aipasslivesync.backend.service;

import aipasslivesync.backend.dto.WorkflowResultDto;
import aipasslivesync.backend.entity.Event;
import aipasslivesync.backend.enums.EventStatus;
import aipasslivesync.backend.repository.EventRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class EventWorkerService {

    private static final Logger log = LoggerFactory.getLogger(EventWorkerService.class);

    private final QueueService queueService;
    private final EventService eventService;
    private final EventLogService eventLogService;
    private final WorkflowEngineService workflowEngine;
    private final MetricsService metricsService;
    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;

    public EventWorkerService(QueueService queueService,
                              EventService eventService,
                              EventLogService eventLogService,
                              WorkflowEngineService workflowEngine,
                              MetricsService metricsService,
                              EventRepository eventRepository,
                              ObjectMapper objectMapper) {
        this.queueService = queueService;
        this.eventService = eventService;
        this.eventLogService = eventLogService;
        this.workflowEngine = workflowEngine;
        this.metricsService = metricsService;
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 1000)
    public void pollAndProcess() {
        String eventIdStr = queueService.dequeue();
        if (eventIdStr == null) return;

        UUID eventId = UUID.fromString(eventIdStr);
        processEvent(eventId);
    }

    private void processEvent(UUID eventId) {
        Timer.Sample timer = metricsService.startTimer();

        try {
            Optional<Event> opt = eventRepository.findById(eventId);
            if (opt.isEmpty()) {
                log.warn("Event {} not found in database — skipping", eventId);
                return;
            }

            Event event = opt.get();
            if (event.getStatus() != EventStatus.PENDING) {
                log.debug("Event {} has status {} — skipping", eventId, event.getStatus());
                return;
            }

            eventService.markProcessing(eventId);
            eventLogService.info(eventId, "Processing started");

            Map<String, Object> payload = objectMapper.readValue(
                    event.getPayload(), new TypeReference<>() {});

            eventLogService.info(eventId, "Executing workflow: " + event.getEventType());
            WorkflowResultDto result = workflowEngine.execute(event.getEventType(), payload);

            eventService.markProcessed(eventId, result);
            eventLogService.info(eventId, "Workflow executed — decision: " + result.decision());
            eventLogService.info(eventId, "Result generated: " + result.reason());
            metricsService.recordProcessed();

        } catch (Exception e) {
            log.error("Failed to process event {}: {}", eventId, e.getMessage(), e);
            eventService.markFailed(eventId, e.getMessage());
            eventLogService.error(eventId, "Processing failed: " + e.getMessage());
            metricsService.recordFailed();
        } finally {
            metricsService.stopTimer(timer);
        }
    }
}
