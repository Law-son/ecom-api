package com.eyarko.ecom.infrastructure.web;

import com.github.benmanes.caffeine.cache.Cache;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Enforces HTTP idempotency for selected POST endpoints using Caffeine cache.
 */
@Component
public class IdempotencyFilter extends OncePerRequestFilter {
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String BAD_REQUEST_BODY =
        "{\"status\":\"error\",\"message\":\"Idempotency-Key reuse with different payload\"}";

    private final Cache<String, IdempotencyEntry> idempotencyCache;

    public IdempotencyFilter(Cache<String, IdempotencyEntry> idempotencyCache) {
        this.idempotencyCache = idempotencyCache;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (!StringUtils.hasText(idempotencyKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(cachedRequest);
        byte[] requestBodyBytes = cachedRequest.getCachedBody();

        if (!isApplicableEndpoint(requestWrapper, requestBodyBytes)) {
            filterChain.doFilter(requestWrapper, response);
            return;
        }

        String requestHash = sha256Hex(requestBodyBytes);
        synchronized (idempotencyKey.intern()) {
            IdempotencyEntry existingEntry = idempotencyCache.getIfPresent(idempotencyKey);
            if (existingEntry != null) {
                if (!existingEntry.getRequestHash().equals(requestHash)) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.getWriter().write(BAD_REQUEST_BODY);
                    return;
                }
                writeCachedResponse(response, existingEntry);
                return;
            }

            ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
            filterChain.doFilter(requestWrapper, responseWrapper);

            int status = responseWrapper.getStatus();
            String body = new String(responseWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
            if (shouldCacheStatus(status)) {
                idempotencyCache.put(idempotencyKey, new IdempotencyEntry(requestHash, status, body));
            }
            responseWrapper.copyBodyToResponse();
        }
    }

    private boolean isApplicableEndpoint(HttpServletRequest request, byte[] requestBodyBytes) {
        String uri = request.getRequestURI();
        if ("/api/v1/orders".equals(uri) || "/api/v1/cart/items".equals(uri) || "/api/v1/reviews".equals(uri)) {
            return true;
        }
        if ("/graphql".equals(uri)) {
            String body = new String(requestBodyBytes, StandardCharsets.UTF_8);
            return body.contains("createOrder") || body.contains("addReview");
        }
        return false;
    }

    private void writeCachedResponse(HttpServletResponse response, IdempotencyEntry entry) throws IOException {
        response.setStatus(entry.getHttpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String cachedBody = entry.getResponseBody() == null ? "" : entry.getResponseBody();
        response.getWriter().write(cachedBody);
    }

    private boolean shouldCacheStatus(int status) {
        return (status >= 200 && status < 300) || (status >= 400 && status < 500);
    }

    private String sha256Hex(byte[] bodyBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bodyBytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }
}

