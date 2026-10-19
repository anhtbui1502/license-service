package com.vht.license.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.vht.license.common.ApiError;
import com.vht.license.ratelimit.RateLimitProperties.KeyType;
import com.vht.license.ratelimit.RateLimitProperties.Rule;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limit token-bucket trong bộ nhớ (1 instance). Mỗi request bị trừ token ở mọi rule khớp;
 * hết token ở bất kỳ rule nào thì trả 429 kèm Retry-After.
 * Chạy nhiều instance thì chuyển sang bucket4j + Redis.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimitProperties props;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .maximumSize(100_000)
            .build();

    public RateLimitFilter(RateLimitProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !props.enabled();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        for (Rule rule : props.rules()) {
            if (!matches(rule, request.getMethod(), path)) {
                continue;
            }
            String key = rule.name() + "|" + resolveKey(rule.key(), request);
            Bucket bucket = buckets.get(key, k -> newBucket(rule));
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            if (!probe.isConsumed()) {
                long waitSeconds = Math.max(1, Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds() + 1);
                log.warn("Rate limit '{}' vượt ngưỡng: key={}, path={}", rule.name(), key, path);
                reject(response, waitSeconds);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean matches(Rule rule, String method, String path) {
        if (rule.method() != null && !rule.method().isBlank() && !rule.method().equalsIgnoreCase(method)) {
            return false;
        }
        return matcher.match(rule.path(), path);
    }

    private static String resolveKey(KeyType type, HttpServletRequest request) {
        if (type == KeyType.USER) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
                return "u:" + auth.getName();
            }
        }
        return "ip:" + request.getRemoteAddr();
    }

    private static Bucket newBucket(Rule rule) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(rule.capacity())
                        .refillGreedy(rule.capacity(), rule.period()).build())
                .build();
    }

    private void reject(HttpServletResponse response, long waitSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(waitSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), new ApiError("RATE_LIMITED",
                "Quá nhiều yêu cầu, vui lòng thử lại sau " + waitSeconds + " giây", waitSeconds));
    }
}
