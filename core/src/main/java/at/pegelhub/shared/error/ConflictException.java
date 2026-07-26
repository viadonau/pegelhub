package at.pegelhub.shared.error;

/**
 * Exception for valid requests that cannot be completed in the current state.
 */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(ApiErrorCode.CONFLICT, message);
    }
}
