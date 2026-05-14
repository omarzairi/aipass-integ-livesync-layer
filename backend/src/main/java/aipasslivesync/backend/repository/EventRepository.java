package aipasslivesync.backend.repository;

import aipasslivesync.backend.entity.Event;
import aipasslivesync.backend.enums.EventStatus;
import aipasslivesync.backend.enums.EventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    Page<Event> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Event> findByEventTypeOrderByCreatedAtDesc(EventType eventType, Pageable pageable);

    Page<Event> findByStatusOrderByCreatedAtDesc(EventStatus status, Pageable pageable);

    Page<Event> findByEventTypeAndStatusOrderByCreatedAtDesc(EventType eventType, EventStatus status, Pageable pageable);

    List<Event> findByStatusAndRetryCountLessThan(EventStatus status, int maxRetries);

    long countByStatus(EventStatus status);
}
