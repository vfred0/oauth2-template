package lt.satsyuk.api.http_errors.request_body.capture;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class RequestBodyCaptureConfig {

    @Bean
    public FilterRegistrationBean<RequestBodyCaptureFilter> requestBodyCaptureFilterRegistration() {
        var registration = new FilterRegistrationBean<>(new RequestBodyCaptureFilter());
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
