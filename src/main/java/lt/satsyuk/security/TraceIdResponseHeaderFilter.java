package lt.satsyuk.security;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class TraceIdResponseHeaderFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String MDC_TRACE_ID_KEY = "traceId";

    private final Tracer tracer;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveTraceId();
        if (StringUtils.hasText(traceId)) {
            response.setHeader(TRACE_ID_HEADER, traceId);
        }

        filterChain.doFilter(request, response);

        if (!response.containsHeader(TRACE_ID_HEADER)) {
            String lateTraceId = resolveTraceId();
            if (StringUtils.hasText(lateTraceId)) {
                response.setHeader(TRACE_ID_HEADER, lateTraceId);
            }
        }
    }

    private String resolveTraceId() {
        Span span = tracer.currentSpan();
        if (span != null) {
            TraceContext context = span.context();
            if (StringUtils.hasText(context.traceId())) {
                return context.traceId();
            }
        }

        String traceIdFromMdc = MDC.get(MDC_TRACE_ID_KEY);
        return StringUtils.hasText(traceIdFromMdc) ? traceIdFromMdc : null;
    }
}


