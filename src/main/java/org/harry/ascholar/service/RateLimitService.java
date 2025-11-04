// src/main/java/org/harry/ascholar/service/RateLimitService.java
package org.harry.ascholar.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RedisTemplate<String, Object> redisTemplate;

    public boolean tryAcquire(String key, Duration duration) {
        return tryAcquire(key, duration, 1);
    }

    public boolean tryAcquire(String key, Duration duration, int maxRequests) {
        try {
            String redisKey = "rate_limit:" + key;
            Long current = redisTemplate.opsForValue().increment(redisKey);

            if (current == null) {
                log.error("Redis increment returned null for key: {}", redisKey);
                return false;
            }

            if (current == 1) {
                // First request - set expiration
                redisTemplate.expire(redisKey, duration.toSeconds(), TimeUnit.SECONDS);
            }

            boolean allowed = current <= maxRequests;

            if (!allowed) {
                log.warn("Rate limit exceeded for key: {}", key);
            }

            return allowed;

        } catch (Exception e) {
            log.error("Rate limit check failed for key: {}", key, e);
            // Fail open in case of Redis issues to avoid blocking users
            return true;
        }
    }

    public long getRemainingRequests(String key, Duration duration, int maxRequests) {
        try {
            String redisKey = "rate_limit:" + key;
            Object currentObj = redisTemplate.opsForValue().get(redisKey);

            if (currentObj == null) {
                return maxRequests;
            }

            long current = Long.parseLong(currentObj.toString());
            return Math.max(0, maxRequests - current);

        } catch (Exception e) {
            log.error("Failed to get remaining requests for key: {}", key, e);
            return maxRequests;
        }
    }

    public void resetLimit(String key) {
        try {
            String redisKey = "rate_limit:" + key;
            redisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.error("Failed to reset rate limit for key: {}", key, e);
        }
    }

    public boolean tryAcquireSlidingWindow(String key, Duration window, int maxRequests) {
        try {
            long currentTime = System.currentTimeMillis();
            long windowSize = window.toMillis();
            String redisKey = "rate_limit_sliding:" + key;

            // Remove outdated requests
            redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, currentTime - windowSize);

            // Count current requests in window
            Long count = redisTemplate.opsForZSet().count(redisKey, currentTime - windowSize, currentTime);

            if (count == null) {
                return false;
            }

            if (count >= maxRequests) {
                log.warn("Sliding window rate limit exceeded for key: {}", key);
                return false;
            }

            // Add current request
            redisTemplate.opsForZSet().add(redisKey, String.valueOf(currentTime), currentTime);
            redisTemplate.expire(redisKey, window.toSeconds(), TimeUnit.SECONDS);

            return true;

        } catch (Exception e) {
            log.error("Sliding window rate limit check failed for key: {}", key, e);
            return true; // Fail open
        }
    }
}