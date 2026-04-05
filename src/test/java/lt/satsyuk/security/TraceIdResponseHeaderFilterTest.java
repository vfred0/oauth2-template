package lt.satsyuk.security;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceIdResponseHeaderFilterTest {

    @Mock
    private Tracer tracer;

    @Mock
    private Span span;

    @Mock
    private TraceContext traceContext;

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void addsHeaderFromCurrentSpanTraceId() throws Exception {
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn("trace-from-span");

        TraceIdResponseHeaderFilter filter = new TraceIdResponseHeaderFilter(tracer);
        MockHttpServletResponse response = doFilter(filter);

        assertThat(response.getHeader(TraceIdResponseHeaderFilter.TRACE_ID_HEADER)).isEqualTo("trace-from-span");
    }

    @Test
    void fallsBackToMdcWhenCurrentSpanMissing() throws Exception {
        when(tracer.currentSpan()).thenReturn(null);
        MDC.put("traceId", "trace-from-mdc");

        TraceIdResponseHeaderFilter filter = new TraceIdResponseHeaderFilter(tracer);
        MockHttpServletResponse response = doFilter(filter);

        assertThat(response.getHeader(TraceIdResponseHeaderFilter.TRACE_ID_HEADER)).isEqualTo("trace-from-mdc");
    }

    @Test
    void doesNotAddHeaderWhenTraceIdUnavailable() throws Exception {
        when(tracer.currentSpan()).thenReturn(null);

        TraceIdResponseHeaderFilter filter = new TraceIdResponseHeaderFilter(tracer);
        MockHttpServletResponse response = doFilter(filter);

        assertThat(response.getHeader(TraceIdResponseHeaderFilter.TRACE_ID_HEADER)).isNull();
    }

    @Test
    void addsHeaderWhenTraceIdAppearsAfterFilterChain() throws Exception {
        when(tracer.currentSpan()).thenReturn(null);

        TraceIdResponseHeaderFilter filter = new TraceIdResponseHeaderFilter(tracer);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/clients");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> MDC.put("traceId", "late-trace-id"));

        assertThat(response.getHeader(TraceIdResponseHeaderFilter.TRACE_ID_HEADER)).isEqualTo("late-trace-id");
    }

    private MockHttpServletResponse doFilter(TraceIdResponseHeaderFilter filter) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/clients");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}

