package tech.lokum.parkinglot.config.logging;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replaces passwords, tokens and other secrets in log messages with {@code ****}.
 *
 * <p>logback-spring.xml registers it for {@code %m}, {@code %msg} and {@code %message}, so every
 * log line is masked whatever pattern prints it. This is a safety net: code should still never log
 * a secret on purpose.
 */
public class SensitiveDataMaskingConverter extends MessageConverter {

    static final String MASK = "****";

    // key=value, key: value and "key":"value", where the key contains a sensitive word
    // (password, DB_PASSWORD, accessToken, api-key, Authorization, ...). A quoted value is matched up
    // to its closing quote, so spaces and escaped quotes inside it are masked too.
    private static final Pattern KEY_VALUE = Pattern.compile(
            "(?i)([\"']?[\\w.-]*(?:password|passwd|pwd|secret|token|api[_-]?key|authorization|credential)"
                    + "[\\w.-]*[\"']?\\s*[:=]\\s*)"
                    + "(\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*'"
                    + "|(?:(?:bearer|basic)\\s+)?[^\\s\"',;&}\\]]+)");

    private static final Pattern BEARER = Pattern.compile("(?i)\\b(bearer|basic)\\s+[\\w.~+/-]+=*");

    private static final Pattern JWT = Pattern.compile("\\beyJ[\\w-]+\\.[\\w-]+\\.[\\w-]+");

    @Override
    public String convert(ILoggingEvent event) {
        return mask(super.convert(event));
    }

    static String mask(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        String masked = KEY_VALUE.matcher(message)
                .replaceAll(m -> Matcher.quoteReplacement(m.group(1) + maskValue(m.group(2))));
        masked = BEARER.matcher(masked).replaceAll("$1 " + MASK);
        return JWT.matcher(masked).replaceAll(MASK);
    }

    // Keeps the quotes around a quoted value so JSON stays readable.
    private static String maskValue(String value) {
        char first = value.charAt(0);
        return first == '"' || first == '\'' ? first + MASK + first : MASK;
    }
}
