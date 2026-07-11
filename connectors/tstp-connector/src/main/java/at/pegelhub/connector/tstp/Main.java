package at.pegelhub.connector.tstp;

import at.pegelhub.lib.runtime.ConnectorApplication;

public class Main {
    public static void main(String[] args) {
        ConnectorApplication.run(args, new TstpConnectorModule());
    }
}
