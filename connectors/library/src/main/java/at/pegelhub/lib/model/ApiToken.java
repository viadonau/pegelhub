package at.pegelhub.lib.model;

/**
 * The model class used to send and receive {@code ApiToken} objects.
 */
public class ApiToken {
    private String apiKey;

    public ApiToken() {
        this.apiKey = "";
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
