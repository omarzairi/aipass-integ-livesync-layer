package aipasslivesync.backend.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final Counter eventsReceived;
    private final Counter eventsProcessed;
    private final Counter eventsFailed;
    private final Timer processingDuration;

    public MetricsService(MeterRegistry registry) {
        eventsReceived = Counter.builder("aipass.events.received")
                .description("Total events received")
                .register(registry);
        eventsProcessed = Counter.builder("aipass.events.processed")
                .description("Total events successfully processed")
                .register(registry);
        eventsFailed = Counter.builder("aipass.events.failed")
                .description("Total events that failed processing")
                .register(registry);
        processingDuration = Timer.builder("aipass.events.processing.duration")
                .description("Event processing duration")
                .register(registry);
    }

    public void recordReceived() { eventsReceived.increment(); }
    public void recordProcessed() { eventsProcessed.increment(); }
    public void recordFailed() { eventsFailed.increment(); }
    public Timer.Sample startTimer() { return Timer.start(); }
    public void stopTimer(Timer.Sample sample) { sample.stop(processingDuration); }

    public double getReceivedCount() { return eventsReceived.count(); }
    public double getProcessedCount() { return eventsProcessed.count(); }
    public double getFailedCount() { return eventsFailed.count(); }
}
