package at.pegelhub.connector.iec.config;

import com.fasterxml.jackson.annotation.JsonCreator;

public record IecIngestionConfig(Mode mode) {
    public enum Mode {
        ALL, LATEST;

        @JsonCreator
        public static Mode from(String value) {
            return switch (value) {
                case "all" -> ALL;
                case "latest" -> LATEST;
                default -> throw new IllegalArgumentException("ingestion.mode must be all or latest");
            };
        }
    }

    public IecIngestionConfig {
        if (mode == null) throw new IllegalArgumentException("ingestion.mode is required");
    }
}
