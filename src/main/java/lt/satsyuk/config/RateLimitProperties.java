package lt.satsyuk.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    @NotBlank
    private String loginPath;

    @NotBlank
    private String clientsPathPrefix;

    @NotBlank
    private String rateLimitedClientId;

    @Valid
    private Rule login = new Rule();

    @Valid
    private Rule clients = new Rule();

    @Getter
    @Setter
    public static class Rule {

        @Min(1)
        private long capacity;

        @Min(1)
        private long windowSeconds;
    }
}

