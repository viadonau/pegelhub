package at.pegelhub.connector.ma;

import at.pegelhub.lib.runtime.ConnectorApplication;

public class MaConnectorApplication {
    public static void main(String[] args) {
        ConnectorApplication.run(args, new MaConnectorModule());
    }
}
