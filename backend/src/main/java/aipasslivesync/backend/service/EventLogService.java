package aipasslivesync.backend.service;

import aipasslivesync.backend.entity.EventLog;
import aipasslivesync.backend.enums.LogLevel;
import aipasslivesync.backend.repository.EventLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EventLogService {

    private static final Logger log = LoggerFactory.getLogger(EventLogService.class);
    private final EventLogRepository eventLogRepository;

    public EventLogService(EventLogRepository eventLogRepository) {
        this.eventLogRepository = eventLogRepository;
    }

    public void log(UUID eventId, LogLevel level, String message) {
        EventLog entry = new EventLog();
        entry.setEventId(eventId);
        entry.setLevel(level);
        entry.setMessage(message);
        eventLogRepository.save(entry);
        log.info("[Event {}] [{}] {}", eventId, level, message);
    }

    public void info(UUID eventId, String message) {
        log(eventId, LogLevel.INFO, message);
    }

    public void warn(UUID eventId, String message) {
        log(eventId, LogLevel.WARN, message);
    }

    public void error(UUID eventId, String message) {
        log(eventId, LogLevel.ERROR, message);
    }

    public Page<EventLog> getLogsByEventId(UUID eventId, Pageable pageable) {
        return eventLogRepository.findByEventIdOrderByTimestampDesc(eventId, pageable);
    }

    public Page<EventLog> getAllLogs(Pageable pageable) {
        return eventLogRepository.findAllByOrderByTimestampDesc(pageable);
    }
}
