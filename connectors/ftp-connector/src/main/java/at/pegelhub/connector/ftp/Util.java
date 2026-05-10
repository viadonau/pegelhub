package at.pegelhub.connector.ftp;

public class Util {
    public static boolean canParseDouble(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
