package lt.satsyuk.service.core.operations.validation;

import lt.satsyuk.api.http_errors.ApiFieldError;
import jakarta.validation.ConstraintViolation;
import java.util.List;
import java.util.Set;

public final class ValidationMapper {

    private ValidationMapper() {}

    public static <T> List<ApiFieldError> mapViolations(Set<ConstraintViolation<T>> violations, int index) {
        return violations.stream()
                .map(v -> mapViolation(v, index))
                .toList();
    }

    public static <T> ApiFieldError mapViolation(ConstraintViolation<T> v, int index) {
        String prop = v.getPropertyPath().toString();
        String path = index >= 0 ? "[" + index + "]." + prop : prop;
        return new ApiFieldError(
                String.valueOf(v.getInvalidValue()),
                prop,
                path,
                mapConstraintToErrorCode(v),
                v.getMessage()
        );
    }

    private static <T> String mapConstraintToErrorCode(ConstraintViolation<T> v) {
        String constraintName = v.getConstraintDescriptor()
                .getAnnotation()
                .annotationType()
                .getSimpleName();
        return "VALIDATION_ERROR_" + camelToSnake(constraintName).toUpperCase();
    }

    private static String camelToSnake(String str) {
        return str == null ? null : str.replaceAll("([a-z])([A-Z]+)", "$1_$2");
    }
}