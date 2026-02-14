package de.dreistrom.config;

import de.dreistrom.common.Idempotent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;
import java.time.Duration;

@Aspect
@Component
@ConditionalOnBean(StringRedisTemplate.class)
@RequiredArgsConstructor
@Slf4j
public class IdempotencyAspect {

    private static final String HEADER_NAME = "Idempotency-Key";
    private static final String KEY_PREFIX = "idempotency:";

    private final StringRedisTemplate redisTemplate;

    @Around("@annotation(idempotent)")
    public Object checkIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attrs.getRequest();
        String idempotencyKey = request.getHeader(HEADER_NAME);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return joinPoint.proceed();
        }

        String redisKey = KEY_PREFIX + idempotencyKey;

        try {
            Boolean wasAbsent = redisTemplate.opsForValue()
                    .setIfAbsent(redisKey, "pending", Duration.ofHours(idempotent.ttlHours()));

            if (Boolean.FALSE.equals(wasAbsent)) {
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                        HttpStatus.CONFLICT, "Duplicate request: Idempotency-Key already used");
                problem.setTitle("Conflict");
                problem.setType(URI.create("about:blank"));
                return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
            }
        } catch (Exception e) {
            log.warn("Redis unavailable for idempotency check, proceeding without: {}", e.getMessage());
        }

        return joinPoint.proceed();
    }
}
