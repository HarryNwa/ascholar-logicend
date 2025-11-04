package org.harry.ascholar.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class RequestContextHelper {

    private static final Logger log = LoggerFactory.getLogger(RequestContextHelper.class);

    /**
     * Get client IP address - Spring MVC only
     */
    public static String getClientIp() {
        try {
            return getClientIpMvc();
        } catch (Exception e) {
            log.debug("Could not determine client IP - no MVC context available");
            return "unknown";
        }
    }

    /**
     * Get user agent - Spring MVC only
     */
    public static String getUserAgent() {
        try {
            return getUserAgentMvc();
        } catch (Exception e) {
            log.debug("Could not determine user agent - no MVC context available");
            return "unknown";
        }
    }

    /**
     * Spring MVC implementation for client IP
     */
    private static String getClientIpMvc() {
        HttpServletRequest request = getCurrentHttpRequest();
        return extractClientIpFromRequest(request);
    }

    /**
     * Spring MVC implementation for user agent
     */
    private static String getUserAgentMvc() {
        HttpServletRequest request = getCurrentHttpRequest();
        return request.getHeader("User-Agent");
    }

    /**
     * Get current HTTP request
     */
    private static HttpServletRequest getCurrentHttpRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes)
                RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest();
    }

    /**
     * Extract IP from HttpServletRequest with proxy support
     */
    private static String extractClientIpFromRequest(HttpServletRequest request) {
        // Check common proxy headers in order
        String[] headersToCheck = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA"
        };

        for (String header : headersToCheck) {
            String ip = request.getHeader(header);
            if (isValidIp(ip)) {
                // Handle multiple IPs in X-Forwarded-For
                if (ip.contains(",")) {
                    return ip.split(",")[0].trim();
                }
                return ip.trim();
            }
        }

        // Fall back to remote address
        return request.getRemoteAddr();
    }

    /**
     * Validate IP address
     */
    private static boolean isValidIp(String ip) {
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            return false;
        }

        // Check for localhost addresses
        if ("0:0:0:0:0:0:0:1".equals(ip) || "127.0.0.1".equals(ip)) {
            return false;
        }

        // Basic IP format validation
        return ip.matches("^([0-9]{1,3}\\.){3}[0-9]{1,3}$") ||
                ip.matches("^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$");
    }
}