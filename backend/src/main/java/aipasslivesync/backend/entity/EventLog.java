package aipasslivesync.backend.entity;

import aipasslivesync.backend.enums.LogLevel;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "event_logs")
public class EventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LogLevel level;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(nullable = false, updatable = false)
    private Instant timestamp;

    @PrePersist
    protected void onCreate() {
        timestamp = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    public LogLevel getLevel() { return level; }
    public void setLevel(LogLevel level) { this.level = level; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Instant getTimestamp() { return timestamp; }
}
