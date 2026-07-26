package at.pegelhub.shared.error;

import static java.util.Objects.requireNonNull;

public abstract class ApiException extends RuntimeException {

    private final ApiErrorCode errorCode;

    protected ApiException(ApiErrorCode errorCode) {
        super(requireNonNull(errorCode).defaultDetail());
        this.errorCode = errorCode;
    }

    protected ApiException(ApiErrorCode errorCode, String message) {
        super(message);
        this.errorCode = requireNonNull(errorCode);
    }

    public ApiErrorCode errorCode() {
        return errorCode;
    }
}
