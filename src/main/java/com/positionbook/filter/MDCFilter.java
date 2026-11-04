package com.positionbook.filter;

import com.github.f4b6a3.uuid.UuidCreator;
import com.positionbook.enums.MDCEnum;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Establishes a request correlation ID in the logging context and response header.
 */
@Component
public class MDCFilter extends OncePerRequestFilter {

    private final String requestHeader;
    private final String responseHeader;
    private final String mdcTokenKey;

    /**
     * Configures the correlation-ID header and MDC key from the shared constants.
     */
    public MDCFilter() {
        this.requestHeader = MDCEnum.DEFAULT_REQUEST_RESPONSE_TOKEN_HEADER.getValue();
        this.responseHeader = MDCEnum.DEFAULT_REQUEST_RESPONSE_TOKEN_HEADER.getValue();
        this.mdcTokenKey = MDCEnum.DEFAULT_MDC_UUID_TOKEN_KEY.getValue();
    }

    /**
     * Adds the correlation ID to the request context and response before continuing
     * the filter chain, then removes it from the thread-local MDC.
     *
     * @param request current HTTP request
     * @param response current HTTP response
     * @param filterChain remaining filters and request handler
     * @throws ServletException if a downstream servlet fails
     * @throws IOException if a downstream request operation fails
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            final String token = extractTokenFromRequest(request);
            MDC.put(mdcTokenKey, token);
            response.addHeader(responseHeader, token);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(mdcTokenKey);
        }
    }

    /**
     * Uses the caller's non-blank correlation ID, or creates one when it is absent.
     *
     * @param request current HTTP request
     * @return the correlation ID for this request
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        final String token;
        if (null == request.getHeader(requestHeader) || request.getHeader(requestHeader).isBlank()) {
            token = UuidCreator.getTimeOrderedEpoch().toString();
        } else {
            token = request.getHeader(requestHeader);
        }
        return token;
    }
}
