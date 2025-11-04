package org.harry.ascholar.utils;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.util.regex.Pattern;

public class SanitizationUtils {
    private static final Safelist ANSWER_SAFELIST = Safelist.none()
            .addTags("br", "p", "div", "span")
            .addAttributes("span", "style")
            .addEnforcedAttribute("span", "style", "color: inherit; background: inherit;");

    private static final Pattern SUSPICIOUS_PATTERNS = Pattern.compile(
            "(?i)(<script|javascript:|onclick|onload|onerror|eval\\(|expression\\()"
    );

    private static final Pattern EXCESSIVE_WHITESPACE = Pattern.compile("\\s{2,}");

    public static String sanitizeHtml(String input) {
        if (input == null) {
            return null;
        }
        return Jsoup.clean(input, ANSWER_SAFELIST);
    }

    public static String trimWhitespace(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        return EXCESSIVE_WHITESPACE.matcher(trimmed).replaceAll(" ");
    }

    public static boolean containsSuspiciousPatterns(String input) {
        if (input == null) {
            return false;
        }
        return SUSPICIOUS_PATTERNS.matcher(input).find();
    }

    public static String escapeForLogging(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}