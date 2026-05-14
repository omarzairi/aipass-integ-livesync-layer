package aipasslivesync.backend.controller;

import aipasslivesync.backend.dto.EventResponse;
import aipasslivesync.backend.dto.WebhookRequest;
import aipasslivesync.backend.entity.Event;
import aipasslivesync.backend.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
@Tag(name = "Webhook", description = "Event ingestion endpoints")
public class WebhookController {

    private final EventService eventService;

    public WebhookController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping("/webhook")
    @Operation(summary = "Ingest an external event", description = "Receives an event, queues it for async processing, and returns immediately")
    public ResponseEntity<EventResponse> ingestEvent(@Valid @RequestBody WebhookRequest request) {
        Event event = eventService.ingest(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(eventService.toResponse(event));
    }
}
