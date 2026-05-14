package aipasslivesync.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI-Pass LiveSync Engine API")
                        .version("1.0.0")
                        .description("Event ingestion, async workflow processing, and monitoring for the AI-Pass integration layer")
                        .contact(new Contact().name("AI-Pass Team")));
    }
}
