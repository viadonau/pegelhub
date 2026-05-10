package at.pegelhub.connector.ftp.fileparsing;

public enum ParserType {
    ASC("asc", ".asc"),
    ZRXP("zrxp", ".zrxp");

    public final String name;
    public final String fileSuffix;

    private ParserType(String name, String fileSuffix) {
        this.name = name;
        this.fileSuffix = fileSuffix;
    }

    public static ParserType valueOfName(String name) {
        for (ParserType type : values()) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        return null;
    }
}
