package at.pegelhub.connector.iec.app;

import at.pegelhub.lib.runtime.ConnectorApplication;

public class IecConnectorApplication {

    public static void main(String[] args)  {
        ConnectorApplication.run(args, new IecConnectorModule());
    }
}
