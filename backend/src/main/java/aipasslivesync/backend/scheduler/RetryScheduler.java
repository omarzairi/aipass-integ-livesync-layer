package aipasslivesync.backend.scheduler;

import aipasslivesync.backend.entity.Event;
import aipasslivesync.backend.enums.EventStatus;
import aipasslivesync.backend.repository.EventRepository;
import aipasslivesync.backend.service.EventLogService;
import aipasslivesync.backend.service.QueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class RetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(RetryScheduler.class);
    private static final int MAX_RETRIES = 3;

    private final EventRepository eventRepository;
    private final QueueService queueService;
    private final EventLogService eventLogService;

    public RetryScheduler(EventRepository eventRepository,
                          QueueService queueService,
                          EventLogService eventLogService) {
        this.eventRepository = eventRepository;
        this.queueService = queueService;
        this.eventLogService = eventLogService;
    }

    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void retryFailedEvents() {
        List<Event> failed = eventRepository.findByStatusAndRetryCountLessThan(
                EventStatus.FAILED, MAX_RETRIES);

        for (Event event : failed) {
            long backoffSeconds = (long) Math.pow(2, event.getRetryCount()) * 10;
            Instant retryAfter = event.getUpdatedAt().plusSeconds(backoffSeconds);

            if (Instant.now().isBefore(retryAfter)) continue;

            event.setStatus(EventStatus.PENDING);
            eventRepository.save(event);
            queueService.enqueue(event.getId());
            eventLogService.warn(event.getId(),
                    "Retry #%d — re-queued after exponential backoff (%ds)".formatted(
                            event.getRetryCount() + 1, backoffSeconds));
            log.info("Retrying event {} (attempt {})", event.getId(), event.getRetryCount() + 1);
        }

        List<Event> deadLetterCandidates = eventRepository.findByStatusAndRetryCountLessThan(
                EventStatus.FAILED, Integer.MAX_VALUE);
        for (Event event : deadLetterCandidates) {
            if (event.getRetryCount() >= MAX_RETRIES && event.getStatus() == EventStatus.FAILED) {
                event.setStatus(EventStatus.DEAD_LETTER);
                eventRepository.save(event);
                eventLogService.error(event.getId(),
                        "Max retries (%d) exceeded — moved to dead letter".formatted(MAX_RETRIES));
                log.warn("Event {} moved to DEAD_LETTER after {} retries", event.getId(), event.getRetryCount());
            }
        }
    }
}
