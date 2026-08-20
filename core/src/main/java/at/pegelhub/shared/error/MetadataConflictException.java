package at.pegelhub.shared.error;

/** A valid metadata request conflicts with the current catalog state. */
public class MetadataConflictException extends RuntimeException {

    public MetadataConflictException(String message) {
        super(message);
    }
}
