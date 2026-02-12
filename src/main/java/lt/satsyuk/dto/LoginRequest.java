package lt.satsyuk.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Username is required")
        @Schema(example = "jdoe")
        String username,
        @NotBlank(message = "Password is required")
        @Schema(example = "P@ssw0rd!")
        String password,
        @NotBlank(message = "ClientId is required")
        @Schema(example = "spring-app")
        String clientId,
        @NotBlank(message = "ClientSecret is required")
        @Schema(example = "spring-app-secret")
        String clientSecret
) {}