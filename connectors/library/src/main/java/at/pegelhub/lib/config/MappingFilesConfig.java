package at.pegelhub.lib.config;

public record MappingFilesConfig(
        String directory
) {
    public static final String DEFAULT_DIRECTORY = "mappings";

    public MappingFilesConfig {
        directory = directory == null || directory.isBlank()
                ? DEFAULT_DIRECTORY
                : directory.trim();
    }

    public static String directoryOf(MappingFilesConfig config) {
        return config == null ? DEFAULT_DIRECTORY : config.directory();
    }
}
