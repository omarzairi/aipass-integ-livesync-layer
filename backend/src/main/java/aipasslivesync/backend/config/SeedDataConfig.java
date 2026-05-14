package aipasslivesync.backend.config;

import aipasslivesync.backend.dto.WebhookRequest;
import aipasslivesync.backend.service.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class SeedDataConfig {

    private static final Logger log = LoggerFactory.getLogger(SeedDataConfig.class);

    @Bean
    public CommandLineRunner seedData(EventService eventService) {
        return args -> {
            log.info("Seeding sample events...");

            eventService.ingest(new WebhookRequest(
                    "invoice.uploaded", "invoice-system",
                    Map.of("invoice_id", "INV-1001", "amount", 1200, "vendor", "Acme Corp")));

            eventService.ingest(new WebhookRequest(
                    "invoice.uploaded", "invoice-system",
                    Map.of("invoice_id", "INV-1002", "amount", 8500, "vendor", "Global Supplies")));

            eventService.ingest(new WebhookRequest(
                    "supplier.updated", "supplier-portal",
                    Map.of("supplier_id", "SUP-501", "status", "active", "rating", 4.5)));

            eventService.ingest(new WebhookRequest(
                    "supplier.updated", "supplier-portal",
                    Map.of("supplier_id", "SUP-502", "status", "suspended", "rating", 1.2)));

            eventService.ingest(new WebhookRequest(
                    "hr.onboarding", "hr-system",
                    Map.of("employee_name", "Alice Johnson", "department", "Engineering", "start_date", "2026-06-01")));

            eventService.ingest(new WebhookRequest(
                    "hr.onboarding", "hr-system",
                    Map.of("employee_name", "Bob Smith", "department", "Marketing")));

            eventService.ingest(new WebhookRequest(
                    "anomaly.alert", "monitoring-system",
                    Map.of("anomaly_type", "transaction_spike", "severity", "critical", "confidence", 0.95)));

            eventService.ingest(new WebhookRequest(
                    "anomaly.alert", "monitoring-system",
                    Map.of("anomaly_type", "login_pattern", "severity", "low", "confidence", 0.3)));

            log.info("Seed data loaded: 8 sample events");
        };
    }
}
