package at.pegelhub.shared.error;

/**
 * Exception for unauthorized requests, such as invalid credentials or lack of permissions.
 */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException() {
        super(ApiErrorCode.UNAUTHORIZED);
    }

    public UnauthorizedException(String message) {
        super(ApiErrorCode.UNAUTHORIZED, message);
    }
}
