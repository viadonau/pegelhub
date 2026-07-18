package at.pegelhub.shared.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.net.URI;

public enum ApiErrorCode {
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Bad request", "The request is invalid."),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Validation failed", "Request validation failed."),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Malformed request", "The request body is malformed."),
    MISSING_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "Missing request parameter", "A required request parameter is missing."),
    INVALID_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "Invalid request parameter", "A request parameter is invalid."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Unauthorized", "Authentication is required."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Forbidden", "Access is denied."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Not found", "The requested resource was not found."),
    CONFLICT(HttpStatus.CONFLICT, "Conflict", "The request conflicts with the current resource state."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed", "The HTTP method is not supported for this resource."),
    NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE, "Not acceptable", "The requested representation is not available."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type", "The request media type is not supported."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", "An unexpected error occurred.");

    private static final String TYPE_BASE = "https://pegelhub.at/problems/";

    private final HttpStatus status;
    private final String title;
    private final String defaultDetail;

    ApiErrorCode(HttpStatus status, String title, String defaultDetail) {
        this.status = status;
        this.title = title;
        this.defaultDetail = defaultDetail;
    }

    public HttpStatus status() {
        return status;
    }

    public String title() {
        return title;
    }

    public String defaultDetail() {
        return defaultDetail;
    }

    public URI type() {
        return URI.create(TYPE_BASE + name().toLowerCase().replace('_', '-'));
    }

    public static ApiErrorCode fromStatus(HttpStatusCode status) {
        if (status.isSameCodeAs(HttpStatus.BAD_REQUEST)) {
            return BAD_REQUEST;
        }
        if (status.isSameCodeAs(HttpStatus.UNAUTHORIZED)) {
            return UNAUTHORIZED;
        }
        if (status.isSameCodeAs(HttpStatus.FORBIDDEN)) {
            return FORBIDDEN;
        }
        if (status.isSameCodeAs(HttpStatus.NOT_FOUND)) {
            return NOT_FOUND;
        }
        if (status.isSameCodeAs(HttpStatus.CONFLICT)) {
            return CONFLICT;
        }
        if (status.isSameCodeAs(HttpStatus.METHOD_NOT_ALLOWED)) {
            return METHOD_NOT_ALLOWED;
        }
        if (status.isSameCodeAs(HttpStatus.NOT_ACCEPTABLE)) {
            return NOT_ACCEPTABLE;
        }
        if (status.isSameCodeAs(HttpStatus.UNSUPPORTED_MEDIA_TYPE)) {
            return UNSUPPORTED_MEDIA_TYPE;
        }
        if (status.is5xxServerError()) {
            return INTERNAL_SERVER_ERROR;
        }
        return BAD_REQUEST;
    }
}
