package lt.satsyuk.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CreateClientRequestTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
    }

    @Test
    void validRequestHasNoViolations() {
        CreateClientRequest request = new CreateClientRequest(
                "John",
                "Doe",
                "+12345678901"
        );

        Set<ConstraintViolation<CreateClientRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void blankFirstNameIsRejected() {
        CreateClientRequest request = new CreateClientRequest(
                " ",
                "Doe",
                "+12345678901"
        );

        Set<ConstraintViolation<CreateClientRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anySatisfy(violation -> {
                    assertThat(violation.getPropertyPath()).hasToString("firstName");
                    assertThat(violation.getMessage()).isEqualTo("firstName is required");
                });
    }

    @Test
    void blankLastNameIsRejected() {
        CreateClientRequest request = new CreateClientRequest(
                "John",
                "",
                "+12345678901"
        );

        Set<ConstraintViolation<CreateClientRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anySatisfy(violation -> {
                    assertThat(violation.getPropertyPath()).hasToString("lastName");
                    assertThat(violation.getMessage()).isEqualTo("lastName is required");
                });
    }

    @Test
    void blankPhoneIsRejected() {
        CreateClientRequest request = new CreateClientRequest(
                "John",
                "Doe",
                ""
        );

        Set<ConstraintViolation<CreateClientRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anySatisfy(violation -> {
                    assertThat(violation.getPropertyPath()).hasToString("phone");
                    assertThat(violation.getMessage()).isEqualTo("phone is required");
                });
    }

    @Test
    void firstNameOverMaxLengthIsRejected() {
        String tooLong = "a".repeat(101);
        CreateClientRequest request = new CreateClientRequest(
                tooLong,
                "Doe",
                "+12345678901"
        );

        Set<ConstraintViolation<CreateClientRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anySatisfy(violation -> assertThat(violation.getPropertyPath()).hasToString("firstName"));
    }

    @Test
    void lastNameOverMaxLengthIsRejected() {
        String tooLong = "b".repeat(101);
        CreateClientRequest request = new CreateClientRequest(
                "John",
                tooLong,
                "+12345678901"
        );

        Set<ConstraintViolation<CreateClientRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anySatisfy(violation -> assertThat(violation.getPropertyPath()).hasToString("lastName"));
    }

    @Test
    void phonePatternIsRejected() {
        CreateClientRequest request = new CreateClientRequest(
                "John",
                "Doe",
                "12-345"
        );

        Set<ConstraintViolation<CreateClientRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anySatisfy(violation -> {
                    assertThat(violation.getPropertyPath()).hasToString("phone");
                    assertThat(violation.getMessage()).isEqualTo("phone must be valid");
                });
    }

    @Test
    void phonePatternAcceptsMinAndMaxLengths() {
        CreateClientRequest min = new CreateClientRequest(
                "John",
                "Doe",
                "1234567"
        );
        CreateClientRequest max = new CreateClientRequest(
                "John",
                "Doe",
                "+123456789012345"
        );

        assertThat(validator.validate(min)).isEmpty();
        assertThat(validator.validate(max)).isEmpty();
    }
}

