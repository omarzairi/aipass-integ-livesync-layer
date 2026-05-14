package aipasslivesync.backend.service;

import aipasslivesync.backend.dto.EventResponse;
import aipasslivesync.backend.dto.WebhookRequest;
import aipasslivesync.backend.dto.WorkflowResultDto;
import aipasslivesync.backend.entity.Event;
import aipasslivesync.backend.enums.EventStatus;
import aipasslivesync.backend.enums.EventType;
import aipasslivesync.backend.exception.EventNotFoundException;
import aipasslivesync.backend.repository.EventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class EventService {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    private final EventRepository eventRepository;
    private final QueueService queueService;
    private final EventLogService eventLogService;
    private final MetricsService metricsService;
    private final ObjectMapper objectMapper;

    public EventService(EventRepository eventRepository,
                        QueueService queueService,
                        EventLogService eventLogService,
                        MetricsService metricsService,
                        ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.queueService = queueService;
        this.eventLogService = eventLogService;
        this.metricsService = metricsService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Event ingest(WebhookRequest request) {
        EventType eventType = parseEventType(request.event_type());

        Event event = new Event();
        event.setEventType(eventType);
        event.setSource(request.source());
        event.setPayload(toJson(request.payload()));
        event.setStatus(EventStatus.PENDING);

        event = eventRepository.save(event);
        metricsService.recordReceived();
        eventLogService.info(event.getId(), "Event received: " + request.event_type());

        queueService.enqueue(event.getId());
        eventLogService.info(event.getId(), "Event queued for processing");

        return event;
    }

    public Page<EventResponse> getAllEvents(Pageable pageable) {
        return eventRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toResponse);
    }

    public EventResponse getEvent(UUID id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));
        return toResponse(event);
    }

    @Transactional
    public Event replay(UUID id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));

        event.setStatus(EventStatus.PENDING);
        event.setWorkflowResult(null);
        event = eventRepository.save(event);

        queueService.enqueue(event.getId());
        eventLogService.info(event.getId(), "Event replayed — re-queued for processing");

        return event;
    }

    @Transactional
    public void markProcessing(UUID id) {
        eventRepository.findById(id).ifPresent(e -> {
            e.setStatus(EventStatus.PROCESSING);
            eventRepository.save(e);
        });
    }

    @Transactional
    public void markProcessed(UUID id, WorkflowResultDto result) {
        eventRepository.findById(id).ifPresent(e -> {
            e.setStatus(EventStatus.PROCESSED);
            e.setWorkflowResult(toJson(result));
            eventRepository.save(e);
        });
    }

    @Transactional
    public void markFailed(UUID id, String reason) {
        eventRepository.findById(id).ifPresent(e -> {
            e.setStatus(EventStatus.FAILED);
            e.setRetryCount(e.getRetryCount() + 1);
            eventRepository.save(e);
        });
    }

    public long countByStatus(EventStatus status) {
        return eventRepository.countByStatus(status);
    }

    public EventResponse toResponse(Event event) {
        return new EventResponse(
                event.getId(),
                event.getEventType(),
                event.getSource(),
                parseJsonSafe(event.getPayload()),
                event.getStatus(),
                parseJsonSafe(event.getWorkflowResult()),
                event.getRetryCount(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }

    private EventType parseEventType(String raw) {
        String normalized = raw.toUpperCase().replace(".", "_");
        try {
            return EventType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported event type: " + raw +
                    ". Supported: invoice.uploaded, supplier.updated, hr.onboarding, anomaly.alert");
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON serialization failed", e);
        }
    }

    private Object parseJsonSafe(String json) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            return json;
        }
    }
}
