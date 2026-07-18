package at.pegelhub.access.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resource types that can be targeted by an access grant.", enumAsRef = true)
public enum AccessResourceType {
    STATION,
    TIME_SERIES
}
