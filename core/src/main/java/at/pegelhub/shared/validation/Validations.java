package at.pegelhub.shared.validation;

import java.util.Collection;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Validations, which can be used to check the quality of members in data classes
 */
public class Validations {

    private Validations() {
        throw new IllegalStateException("utility class can not be initialized.");
    }

    public static String requireNotEmpty(String text) {
        requireNonNull(text);
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Text should not be empty");
        }
        return text;
    }

    public static <K, V> Map<K, V> requireNotEmpty(Map<K, V> container) {
        requireNonNull(container);
        if (container.isEmpty()) {
            throw new IllegalArgumentException("Collection should not be empty");
        }
        return container;
    }

    public static <E> Collection<E> requireNotEmpty(Collection<E> container) {
        requireNonNull(container);
        if (container.isEmpty()) {
            throw new IllegalArgumentException("Collection should not be empty");
        }
        return container;
    }

    public static String requireSEThan(String value, int length) {
        requireNonNull(value);
        if (value.length() > length) {
            throw new IllegalArgumentException("String is too long");
        }
        return value;
    }

    public static double requireSEThan(double value, int length) {
        if (value > length) {
            throw new IllegalArgumentException("String is too long");
        }
        return value;
    }

    public static double requirePositive(double value) {
        if (value < 0) {
            throw new IllegalArgumentException("Number must be positive");
        }
        return value;
    }

    public static long requirePositive(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Number must be positive");
        }
        return value;
    }

    public static String normalizeRequired(String value, String message) {
        requireNonNull(value);
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    public static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        value = value.trim();
        return value.isBlank() ? null : value;
    }
}
