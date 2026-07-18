package at.pegelhub.connector.tstp.client;

public final class TstpClientException extends RuntimeException {
    public TstpClientException(String message) {
        super(message);
    }

    public TstpClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
