package at.pegelhub.connector.ma.jni;

public class RevPiReaderImpl implements RevPiReader {

    static {
        System.loadLibrary("RevPiReader");
    }

    public native int resolveOffsetByName(String variableName);
    public native int readFromOffset(int offset);
    public native void close();
}
