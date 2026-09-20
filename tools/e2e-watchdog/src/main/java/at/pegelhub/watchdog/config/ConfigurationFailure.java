package at.pegelhub.watchdog.config;

/** Only application-authored messages may cross the diagnostic boundary; never wrap raw input here. */
public final class ConfigurationFailure extends IllegalArgumentException {
    ConfigurationFailure(String message) {
        super(message);
    }
}
