package at.pegelhub.shared.error;

/**
 * Exception for objects that were requested but do not exist.
 */
public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(ApiErrorCode.NOT_FOUND, message);
    }
}
