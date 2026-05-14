package aipasslivesync.backend.controller;

import aipasslivesync.backend.dto.EventResponse;
import aipasslivesync.backend.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@Tag(name = "Events", description = "Event query and replay endpoints")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    @Operation(summary = "List all events", description = "Paginated list of all events, ordered by creation date descending")
    public Page<EventResponse> getAllEvents(@PageableDefault(size = 20) Pageable pageable) {
        return eventService.getAllEvents(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get event by ID", description = "Returns a single event with its workflow result")
    public EventResponse getEvent(@PathVariable UUID id) {
        return eventService.getEvent(id);
    }

    @PostMapping("/{id}/replay")
    @Operation(summary = "Replay an event", description = "Resets the event to PENDING and re-queues it for processing")
    public ResponseEntity<EventResponse> replayEvent(@PathVariable UUID id) {
        var event = eventService.replay(id);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(eventService.toResponse(event));
    }
}
