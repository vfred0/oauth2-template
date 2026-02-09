package lt.satsyuk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateClientRequest(

        @NotBlank(message = "firstName is required")
        @Size(max = 100)
        String firstName,

        @NotBlank(message = "lastName is required")
        @Size(max = 100)
        String lastName,

        @NotBlank(message = "phone is required")
        @Pattern(regexp = "\\+?\\d{7,15}", message = "phone must be valid")
        String phone
) {}