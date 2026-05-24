package at.pegelhub.shared.error;

/**
 * Exception for unauthorized requests, such as invalid credentials or lack of permissions.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException() {
    }
    public UnauthorizedException(String message) {
        super(message);
    }
}
