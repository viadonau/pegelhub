package at.pegelhub.access.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "openapi.access.access-resource-type.resource-types-that-can-be-targeted-by", enumAsRef = true)
public enum AccessResourceType {
    STATION,
    TIME_SERIES
}
