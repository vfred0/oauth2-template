package lt.satsyuk.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateClientRequest(

        @NotBlank(message = "firstName is required")
        @Size(max = 100)
        @Schema(example = "John")
        String firstName,

        @NotBlank(message = "lastName is required")
        @Size(max = 100)
        @Schema(example = "Doe")
        String lastName,

        @NotBlank(message = "phone is required")
        @Pattern(regexp = "\\+?\\d{7,15}", message = "phone must be valid")
        @Schema(example = "+37060000000")
        String phone
) {}