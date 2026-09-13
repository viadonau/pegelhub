package at.pegelhub.measurement.application;

/** Signals that a bounded read could not prove completeness; callers must not evaluate it as a pass. */
public class MeasurementReadLimitException extends RuntimeException {

    public enum Code { DEADLINE, POINT_LIMIT, INCOMPLETE_WINDOW }

    private final Code code;

    public MeasurementReadLimitException(Code code) {
        super(code.name());
        this.code = code;
    }

    public Code code() {
        return code;
    }
}
