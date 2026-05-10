package at.pegelhub.lib.internal.dto;

import at.pegelhub.lib.model.ApiToken;

public record ApiTokenReceiveDto(String apiKey) {
    public ApiToken toApiToken() {
        var dto = new ApiToken();
        dto.setApiKey(apiKey);
        return dto;
    }
}