package lt.satsyuk.api.http_errors.request_body;

import lt.satsyuk.api.dtos.core.ApiResult;
import lt.satsyuk.api.http_errors.ApiErrorType;
import lt.satsyuk.api.http_errors.ApiFieldError;
import lt.satsyuk.api.http_errors.request_body.capture.RequestBodyReader;
import lt.satsyuk.api.http_errors.request_body.parsing.JsonTokenScanner;
import lt.satsyuk.api.http_errors.request_body.parsing.ScanResult;
import lt.satsyuk.api.http_errors.request_body.processing.JacksonErrorMapper;
import lt.satsyuk.api.http_errors.request_body.processing.SafeBodyProcessor;
import lt.satsyuk.service.core.operations.log.RequestLogService;
import lt.satsyuk.service.core.operations.route.ValidationRouteRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestBodyErrorResolver {

    private static final String NOT_NULL_CODE = "VALIDATION_ERROR_NOT_NULL";

    private final RequestBodyReader bodyReader;
    private final ValidationRouteRegistry routeRegistry;
    private final RequestLogService requestLogService;
    private final SafeBodyProcessor safeBodyProcessor;

    public ApiResult<?> resolve(HttpMessageNotReadableException ex, HttpServletRequest request) {
        var originalBody = bodyReader.read();
        var scanResult = JsonTokenScanner.scan(originalBody);
        var route = routeRegistry.match(request.getMethod(), request.getRequestURI(), scanResult.safeBody());
        var validationErrors = safeBodyProcessor.collectErrors(scanResult.safeBody(), route);
        var allErrors = deduplicate(mergeErrors(ex, scanResult, validationErrors));
        return buildResponse(allErrors, request, scanResult.safeBody());
    }

    private List<ApiFieldError> mergeErrors(HttpMessageNotReadableException ex, ScanResult scanResult,
                                            List<ApiFieldError> validationErrors) {
        boolean usedFallback = !scanResult.hasErrors();
        if (usedFallback && !validationErrors.isEmpty()) return new ArrayList<>(validationErrors);
        var errors = new ArrayList<>(scanResult.errors());
        if (usedFallback) errors.add(JacksonErrorMapper.mapUnreadableException(ex));
        errors.addAll(validationErrors);
        return errors;
    }

    private List<ApiFieldError> deduplicate(List<ApiFieldError> errors) {
        var distinct = errors.stream().distinct().toList();
        var keysWithOtherErrors = new HashSet<String>();
        distinct.stream()
                .filter(e -> !NOT_NULL_CODE.equals(e.code()))
                .forEach(e -> keysWithOtherErrors.add(fieldKey(e.path(), e.property())));
        return distinct.stream()
                .filter(e -> !NOT_NULL_CODE.equals(e.code()) || !keysWithOtherErrors.contains(fieldKey(e.path(), e.property())))
                .toList();
    }

    private String fieldKey(String path, String property) {
        if (path == null) return property;
        int end = path.indexOf(']');
        var prefix = (path.startsWith("[") && end >= 0) ? path.substring(0, end + 1) : "";
        return prefix + (property != null ? property : "");
    }

    private ApiResult<?> buildResponse(List<ApiFieldError> errors, HttpServletRequest request, String safeBody) {
        if (hasServerError(errors)) return ApiResult.error(ApiErrorType.INTERNAL_SERVER_ERROR);
        ApiResult<?> response = isSingleMode(safeBody)
                ? ApiResult.errors(ApiErrorType.BAD_REQUEST, errors)
                : safeBodyProcessor.bulkResponse(errors, safeBody);
        requestLogService.log(request, response);
        return response;
    }

    private boolean hasServerError(List<ApiFieldError> errors) {
        return errors.stream().anyMatch(e -> e.code() != null && e.code().contains("INTERNAL_SERVER_ERROR"));
    }

    private boolean isSingleMode(String safeBody) {
        return safeBody == null || !safeBody.stripLeading().startsWith("[");
    }
}
