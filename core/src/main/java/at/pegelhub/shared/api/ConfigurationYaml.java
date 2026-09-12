package at.pegelhub.shared.api;

import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Bounded, strict decoding into caller-selected configuration records, never document-selected polymorphic types.
 * Record constructors validate shape; callers must still use the ordinary application edit path for cross-record checks.
 */
@Component
public final class ConfigurationYaml {

    private final YAMLMapper mapper = YAMLMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

    public <T> T read(String yaml, Class<T> type) {
        if (yaml == null || yaml.length() > 65_536) {
            throw new IllegalArgumentException("Configuration is too large");
        }

        try {
            T value = mapper.readValue(yaml, type);
            if (value == null) {
                throw new IllegalArgumentException("Configuration document is empty");
            }

            return value;
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Invalid configuration document");
        }
    }

    public String write(Object value) {
        return mapper.writeValueAsString(value);
    }
}
