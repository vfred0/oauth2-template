package lt.satsyuk.observability;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = LoggingCorrelationTest.TestApplication.class,
        properties = {
                "spring.main.web-application-type=none",
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.jpa.autoconfigure.HibernateJpaAutoConfiguration,"
                        + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.quartz.autoconfigure.QuartzAutoConfiguration,"
                        + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
        }
)
@ExtendWith(OutputCaptureExtension.class)
class LoggingCorrelationTest {

    private static final Logger log = LoggerFactory.getLogger(LoggingCorrelationTest.class);

    @Autowired
    private Tracer tracer;

    @Test
    void logLineContainsTraceIdAndSpanIdWhenSpanIsActive(CapturedOutput output) {
        Span span = tracer.nextSpan().name("logging-correlation-test").start();
        try (Tracer.SpanInScope scope = tracer.withSpan(span)) {
            assertThat(scope).isNotNull();
            log.info("correlation-check-message");
        } finally {
            span.end();
        }

        String logs = output.toString();
        assertThat(logs)
                .contains("correlation-check-message")
                .contains("traceId")
                .contains("spanId");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}



