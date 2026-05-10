package at.pegelhub.lib.internal;

import java.util.HashMap;
import java.util.Map;

public class ApplicationPropertiesFactory {

    private static Map<String, ApplicationProperties> instances = new HashMap<>();

    public static ApplicationProperties create(String propertiesFileName) {
        ApplicationProperties props = instances.get(propertiesFileName);
        if (props == null) {
            props = new ApplicationPropertiesImpl(propertiesFileName);
            instances.put(propertiesFileName, props);
        }
        return props;
    }
}
