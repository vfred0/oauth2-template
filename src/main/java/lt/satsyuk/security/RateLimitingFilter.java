package lt.satsyuk.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lt.satsyuk.config.RateLimitProperties;
import lt.satsyuk.dto.ApiResponse;
import lt.satsyuk.service.MessageService;
import lt.satsyuk.service.SecurityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final String RATE_LIMIT_MESSAGE_KEY = "api.error.tooManyRequests";

    private final SecurityService securityService;
    private final MessageService messageService;
    private final RateLimitProperties rateLimitProperties;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final ConcurrentMap<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Bucket> clientBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getServletPath();

        if (isLoginRequest(request, path) && isRateLimited(loginBuckets, loginKey(request), rateLimitProperties.getLogin())) {
            writeRateLimitedResponse(response);
            return;
        }

        if (isClientsRequest(path)
                && rateLimitProperties.getRateLimitedClientId().equals(securityService.clientId())
                && isRateLimited(clientBuckets, clientsKey(), rateLimitProperties.getClients())) {
            writeRateLimitedResponse(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isLoginRequest(HttpServletRequest request, String path) {
        return rateLimitProperties.getLoginPath().equals(path) && "POST".equalsIgnoreCase(request.getMethod());
    }

    private boolean isClientsRequest(String path) {
        return path != null && path.startsWith(rateLimitProperties.getClientsPathPrefix());
    }

    public void clearBuckets() {
        loginBuckets.clear();
        clientBuckets.clear();
    }

    private boolean isRateLimited(ConcurrentMap<String, Bucket> buckets, String key, RateLimitProperties.Rule rule) {
        return !resolveBucket(buckets, key, rule).tryConsume(1);
    }

    private Bucket resolveBucket(ConcurrentMap<String, Bucket> buckets, String key, RateLimitProperties.Rule rule) {
        return buckets.computeIfAbsent(key, ignored -> Bucket.builder()
                .addLimit(Bandwidth.classic(
                        rule.getCapacity(),
                        Refill.intervally(rule.getCapacity(), Duration.ofSeconds(rule.getWindowSeconds()))
                ))
                .build());
    }

    private String loginKey(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        return "login:" + (remoteAddr == null || remoteAddr.isBlank() ? "unknown" : remoteAddr);
    }

    private String clientsKey() {
        return "clients:" + securityService.clientId();
    }

    private void writeRateLimitedResponse(HttpServletResponse response) throws IOException {
        ApiResponse<Void> error = ApiResponse.error(
                ApiResponse.ErrorCode.TOO_MANY_REQUESTS.getCode(),
                messageService.getMessage(RATE_LIMIT_MESSAGE_KEY)
        );

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}



