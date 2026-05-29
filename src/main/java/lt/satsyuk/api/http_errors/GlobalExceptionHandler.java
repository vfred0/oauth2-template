package lt.satsyuk.api.http_errors;

import lt.satsyuk.api.dtos.core.ApiResult;
import lt.satsyuk.api.http_errors.exceptions.AccountNotFoundException;
import lt.satsyuk.api.http_errors.exceptions.AccountOptimisticLockException;
import lt.satsyuk.api.http_errors.exceptions.ApiKeyNotFoundException;
import lt.satsyuk.api.http_errors.exceptions.ClientNotFoundException;
import lt.satsyuk.api.http_errors.exceptions.ClientSearchQueryTooShortException;
import lt.satsyuk.api.http_errors.exceptions.InternalServerErrorException;
import lt.satsyuk.api.http_errors.exceptions.PhoneAlreadyExistsException;
import lt.satsyuk.api.http_errors.exceptions.RequestNotFoundException;
import lt.satsyuk.api.http_errors.request_body.RequestBodyErrorResolver;
import lt.satsyuk.config.api_version.ApiVersion;
import lt.satsyuk.service.core.shared.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.TransactionException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.accept.InvalidApiVersionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageService messageService;
    private final RequestBodyErrorResolver requestBodyErrorResolver;

    // --- Request body / JSON parse (delegated) ---

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        var response = requestBodyErrorResolver.resolve(ex, request);
        return ResponseEntity.status(response.status()).contentType(MediaType.APPLICATION_JSON).body(response);
    }

    // --- Validation ---

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElseGet(() -> messageService.getMessage("error.validation.failed"));
        return respond(ApiErrorType.BAD_REQUEST, message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResult<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return respond(ApiErrorType.BAD_REQUEST,
                messageService.getMessage("error.typeMismatch", new Object[]{String.valueOf(ex.getValue())}));
    }

    // --- Domain ---

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResult<Void>> handleAccessDenied(AccessDeniedException ex) {
        return respond(ApiErrorType.FORBIDDEN, messageService.getMessage("api.error.forbidden"));
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiResult<Void>> handleAccountNotFound(AccountNotFoundException ex) {
        return respond(ApiErrorType.NOT_FOUND, msg(ex.getMessageCode(), ex.getClientId()));
    }

    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<ApiResult<Void>> handleClientNotFound(ClientNotFoundException ex) {
        return respond(ApiErrorType.NOT_FOUND, msg(ex.getMessageCode(), ex.getClientId()));
    }

    @ExceptionHandler(ApiKeyNotFoundException.class)
    public ResponseEntity<ApiResult<Void>> handleApiKeyNotFound(ApiKeyNotFoundException ex) {
        return respond(ApiErrorType.NOT_FOUND, msg(ex.getMessageCode(), ex.getApiKeyId()));
    }

    @ExceptionHandler(RequestNotFoundException.class)
    public ResponseEntity<ApiResult<Void>> handleRequestNotFound(RequestNotFoundException ex) {
        return respond(ApiErrorType.NOT_FOUND, msg(ex.getMessageCode(), ex.getRequestId()));
    }

    @ExceptionHandler(AccountOptimisticLockException.class)
    public ResponseEntity<ApiResult<Void>> handleOptimisticLock(AccountOptimisticLockException ex) {
        return respond(ApiErrorType.CONFLICT, msg(ex.getMessageCode(), ex.getClientId()));
    }

    @ExceptionHandler(PhoneAlreadyExistsException.class)
    public ResponseEntity<ApiResult<Void>> handlePhoneExists(PhoneAlreadyExistsException ex) {
        return respond(ApiErrorType.CONFLICT, msg(ex.getMessageCode(), ex.getPhone()));
    }

    @ExceptionHandler(ClientSearchQueryTooShortException.class)
    public ResponseEntity<ApiResult<Void>> handleSearchQueryTooShort(ClientSearchQueryTooShortException ex) {
        return respond(ApiErrorType.BAD_REQUEST, msg(ex.getMessageCode(), ex.getMinLength()));
    }

    // --- Transport / infrastructure ---

    @ExceptionHandler(InternalServerErrorException.class)
    public ResponseEntity<ApiResult<Void>> handleInternalServerError(InternalServerErrorException ex) {
        return respond(ApiErrorType.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({DataAccessResourceFailureException.class, TransactionException.class, ResourceAccessException.class})
    public ResponseEntity<ApiResult<Void>> handleServiceUnavailable(Exception ex) {
        return respond(ApiErrorType.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler({SocketTimeoutException.class, TimeoutException.class})
    public ResponseEntity<ApiResult<Void>> handleGatewayTimeout(Exception ex) {
        return respond(ApiErrorType.GATEWAY_TIMEOUT);
    }

    @ExceptionHandler(InvalidApiVersionException.class)
    public ResponseEntity<ApiResult<Void>> handleInvalidApiVersion(InvalidApiVersionException ex) {
        return respond(ApiErrorType.INVALID_API_VERSION,
                ApiErrorType.INVALID_API_VERSION.message() + ". Supported versions: " + ApiVersion.all());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResult<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
                                                                     HttpServletRequest request) {
        return respond(ApiErrorType.METHOD_NOT_ALLOWED,
                "Not allowed method [" + ex.getMethod() + "] to " + request.getRequestURI());
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResult<Void>> handleNotFound(NoHandlerFoundException ex) {
        return respond(ApiErrorType.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleGeneric(Exception ex) {
        log.error("Unhandled exception caught by GlobalExceptionHandler", ex);
        return respond(ApiErrorType.INTERNAL_SERVER_ERROR, messageService.getMessage("api.error.internalServerError"));
    }

    private String msg(String code, Object arg) {
        return messageService.getMessage(code, new Object[]{String.valueOf(arg)});
    }

    private ResponseEntity<ApiResult<Void>> respond(ApiErrorType type) {
        return respond(type, type.message());
    }

    private ResponseEntity<ApiResult<Void>> respond(ApiErrorType type, String message) {
        return ResponseEntity.status(type.status())
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResult.error(type, message));
    }
}
