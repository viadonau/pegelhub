package at.pegelhub.connector.ma.jni;


public interface RevPiReader {

    /**
     * Resolves a RevPi variable name to its process image offset.
     *
     * @param variableName variable name as defined in piCtory
     * @return byte offset in the process image
     */
    int resolveOffsetByName(String variableName);

    /**
     * Reads an unsigned, little-endian 16-bit value at a process-image byte offset.
     *
     * @param offset byte offset in the process image
     * @return a value from 0 through 65535
     */
    int readFromOffset(int offset);

    /**
     * Releases native resources and closes the device handle.
     */
    void close();
}
