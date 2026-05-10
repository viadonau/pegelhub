package at.pegelhub.lib;

import at.pegelhub.lib.internal.ApplicationPropertiesFactory;
import at.pegelhub.lib.internal.HttpPegelHubCommunicator;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;

import java.net.URL;

/**
 * This class is used to construct {@code PegelHubCommunicator} instances.
 */
public class PegelHubCommunicatorFactory {
    /**
     * Create a valid {@code PegelHubCommunicator} that connects to the core located at {@param baseUrl}.
     * @param baseUrl the base URL used to connect to a core instance
     * @return a valid {@code PegelHubCommunicator} instance
     */
    public static PegelHubCommunicator create(URL baseUrl) {
        return PegelHubCommunicatorFactory.create(baseUrl, "pegelhub.yaml");
    }
    /**
     * Create a valid {@code PegelHubCommunicator} that connects to the core located at {@param baseUrl}.
     * @param baseUrl the base URL used to connect to a core instance
     * @param propertiesFile path to the properties file
     * @return a valid {@code PegelHubCommunicator} instance
     */
    public static PegelHubCommunicator create(URL baseUrl, String propertiesFile) {
        CloseableHttpClient client = HttpClients.createDefault();
        return new HttpPegelHubCommunicator(client, baseUrl, ApplicationPropertiesFactory.create(propertiesFile));
    }

    /**
     * Create a valid {@code PegelHubCommunicator} that connects to the core located at {@param baseUrl}.
     * @param baseUrl the base URL used to connect to a core instance
     * @param propertiesFile path to the properties file
     * @param measurementRoute an override for the measurement route
     * @param telemetryRoute an override for the telemetry route
     * @param contactRoute an override for the contact route
     * @param connectorRoute an override for the connector route
     * @param tokenRoute an override for the token route
     * @return a valid {@code PegelHubCommunicator} instance
     */
    public static PegelHubCommunicator create(URL baseUrl, String propertiesFile, String measurementRoute, String telemetryRoute, String contactRoute, String connectorRoute,  String tokenRoute,  String takerRoute,  String supplierRoute) {
        CloseableHttpClient client = HttpClients.createDefault();
        return new HttpPegelHubCommunicator(client, baseUrl, ApplicationPropertiesFactory.create(propertiesFile), measurementRoute, telemetryRoute, contactRoute, connectorRoute, tokenRoute, takerRoute, supplierRoute);
    }
}
