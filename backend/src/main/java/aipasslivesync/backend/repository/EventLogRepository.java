package aipasslivesync.backend.repository;

import aipasslivesync.backend.entity.EventLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EventLogRepository extends JpaRepository<EventLog, UUID> {

    Page<EventLog> findByEventIdOrderByTimestampDesc(UUID eventId, Pageable pageable);

    Page<EventLog> findAllByOrderByTimestampDesc(Pageable pageable);
}
