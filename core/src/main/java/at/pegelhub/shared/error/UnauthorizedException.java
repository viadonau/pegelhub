package at.pegelhub.shared.error;

/**
 * Exception for unauthorized requests (e.g. invalid api key or lack of permissions).
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException() {
    }
    public UnauthorizedException(String message) {
        super(message);
    }
}
