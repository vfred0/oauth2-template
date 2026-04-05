package lt.satsyuk.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class OtelLogbackConfig {

    private final OpenTelemetry openTelemetry;

    @PostConstruct
    void installLogbackAppender() {
        // Binds Logback appender to the OpenTelemetry instance configured by Spring Boot.
        OpenTelemetryAppender.install(openTelemetry);
    }
}

