package at.pegelhub.shared.api;

import java.util.List;

public record Page<T>(List<T> items, int offset, int limit, long total) {

    public Page {
        items = List.copyOf(items);
    }

    public static void validate(int offset, int limit) {
        if (offset < 0 || limit < 1 || limit > 200) {
            throw new IllegalArgumentException("Invalid pagination: limit must be 1 to 200");
        }
    }
}
