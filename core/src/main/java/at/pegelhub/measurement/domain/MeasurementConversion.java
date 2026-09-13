package at.pegelhub.measurement.domain;

/**
 * Converts values between one representation and the units used for storage.
 * Both directions use the same fixed conversion settings; no metadata is loaded while converting.
 * Implementations reject non-finite inputs and results rather than returning NaN or infinity.
 *
 * <p>Conversions must only scale or shift values: bucket reads apply them after averaging.
 */
public interface MeasurementConversion {

    /** Returns the unit of the chosen representation. It may differ from the unit used for storage. */
    String unit();

    /**
     * Converts an incoming value to storage units.
     *
     * @throws IllegalArgumentException if the input or converted value is NaN or infinite
     */
    double toCanonical(double value);

    /**
     * Converts a stored value to the chosen output representation.
     *
     * @throws IllegalArgumentException if the input or converted value is NaN or infinite
     */
    double fromCanonical(double value);
}
