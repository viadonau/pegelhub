package org.stm.pegelhub.connector.ma.jni;


public interface RevPiReader {

    /**
     * Resolves a RevPi variable name to its process image offset.
     *
     * @param variableName variable name as defined in piCtory
     * @return byte offset in the process image
     * @throws RuntimeException if resolution fails or the device is unavailable
     */
    int resolveOffsetByName(String variableName);

    /**
     * Reads an int value at the given process image offset. This represents the measurement from an input.
     *
     * @param offset byte offset in the process image
     * @return the read int
     * @throws RuntimeException if the read fails or the device is unavailable
     */
    int readFromOffset(int offset);

    /**
     * Releases native resources and closes the device handle.
     *
     * @throws RuntimeException if closing the device fails
     */
    void close();
}
