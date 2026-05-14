package aipasslivesync.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class QueueService {

    private static final Logger log = LoggerFactory.getLogger(QueueService.class);
    private static final String QUEUE_KEY = "aipass:event-queue";

    private final StringRedisTemplate redisTemplate;
    private final boolean redisAvailable;

    private final ConcurrentLinkedQueue<String> fallbackQueue = new ConcurrentLinkedQueue<>();

    public QueueService(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.redisAvailable = testRedisConnection();
        if (!redisAvailable) {
            log.warn("Redis unavailable — using in-memory fallback queue");
        } else {
            log.info("Redis connected — using Redis-backed queue");
        }
    }

    private boolean testRedisConnection() {
        if (redisTemplate == null) return false;
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void enqueue(UUID eventId) {
        String id = eventId.toString();
        if (redisAvailable) {
            try {
                redisTemplate.opsForList().leftPush(QUEUE_KEY, id);
                log.debug("Enqueued event {} to Redis", id);
                return;
            } catch (Exception e) {
                log.warn("Redis enqueue failed, falling back to in-memory: {}", e.getMessage());
            }
        }
        fallbackQueue.offer(id);
        log.debug("Enqueued event {} to in-memory queue", id);
    }

    public String dequeue() {
        if (redisAvailable) {
            try {
                String id = redisTemplate.opsForList().rightPop(QUEUE_KEY);
                if (id != null) return id;
            } catch (Exception e) {
                log.warn("Redis dequeue failed, falling back to in-memory: {}", e.getMessage());
            }
        }
        return fallbackQueue.poll();
    }

    public long getQueueDepth() {
        long fallbackSize = fallbackQueue.size();
        if (redisAvailable) {
            try {
                Long size = redisTemplate.opsForList().size(QUEUE_KEY);
                return (size != null ? size : 0) + fallbackSize;
            } catch (Exception e) {
                // fall through
            }
        }
        return fallbackSize;
    }

    public boolean isRedisAvailable() {
        return redisAvailable;
    }
}
