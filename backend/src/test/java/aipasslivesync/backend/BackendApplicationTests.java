package aipasslivesync.backend;

import aipasslivesync.backend.dto.WebhookRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BackendApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void healthEndpointReturnsUp() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.uptimeSeconds").isNumber())
                .andExpect(jsonPath("$.queue").isMap())
                .andExpect(jsonPath("$.metrics").isMap());
    }

    @Test
    void webhookAcceptsValidEvent() throws Exception {
        WebhookRequest request = new WebhookRequest(
                "invoice.uploaded", "test-system",
                Map.of("invoice_id", "TEST-001", "amount", 999));

        mockMvc.perform(post("/api/events/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.eventType").value("INVOICE_UPLOADED"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void webhookRejectsInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/events/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listEventsReturnsPaginatedResults() throws Exception {
        mockMvc.perform(get("/api/events").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageable").isMap());
    }

    @Test
    void getEventReturns404ForUnknownId() throws Exception {
        mockMvc.perform(get("/api/events/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    void webhookProcessesAllEventTypes() throws Exception {
        Map<String, Map<String, Object>> types = new java.util.LinkedHashMap<>();
        types.put("invoice.uploaded", Map.of("invoice_id", "T-1", "amount", 100));
        types.put("supplier.updated", Map.of("supplier_id", "S-1", "status", "active", "rating", 4.0));
        types.put("hr.onboarding", Map.of("employee_name", "Test", "department", "Eng", "start_date", "2026-01-01"));
        types.put("anomaly.alert", Map.of("anomaly_type", "spike", "severity", "high", "confidence", 0.85));

        for (var entry : types.entrySet()) {
            WebhookRequest req = new WebhookRequest(entry.getKey(), "test", entry.getValue());
            mockMvc.perform(post("/api/events/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isAccepted());
        }
    }

    @Test
    void logsEndpointReturns200() throws Exception {
        mockMvc.perform(get("/api/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
