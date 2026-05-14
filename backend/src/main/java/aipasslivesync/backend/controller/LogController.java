package aipasslivesync.backend.controller;

import aipasslivesync.backend.dto.EventLogResponse;
import aipasslivesync.backend.entity.EventLog;
import aipasslivesync.backend.service.EventLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/logs")
@Tag(name = "Logs", description = "Event processing logs")
public class LogController {

    private final EventLogService eventLogService;

    public LogController(EventLogService eventLogService) {
        this.eventLogService = eventLogService;
    }

    @GetMapping
    @Operation(summary = "List all logs", description = "Paginated list of all event processing logs")
    public Page<EventLogResponse> getAllLogs(
            @RequestParam(required = false) UUID eventId,
            @PageableDefault(size = 50) Pageable pageable) {

        Page<EventLog> logs = eventId != null
                ? eventLogService.getLogsByEventId(eventId, pageable)
                : eventLogService.getAllLogs(pageable);

        return logs.map(l -> new EventLogResponse(
                l.getId(), l.getEventId(), l.getLevel(), l.getMessage(), l.getTimestamp()));
    }
}
