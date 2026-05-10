package at.pegelhub.connector.tstp.service;

import at.pegelhub.connector.tstp.ConnectorOptions;

import java.io.IOException;
import java.time.Duration;

/**
 * Handles the parsing of the input parameters for the TSTP-Connector
 */
public interface TstpConfigService {

    /**
     * Parses the given arguments from the config file to the needed properties to instantiate a TSTP-Connector
     *
     * @return the parsed ConnectorOptions
     * @throws IOException if an error occurs while reading the properties
     */
    ConnectorOptions getConnectorOptions() throws IOException;

    /**
     * Parses the string to a Duration Object if the format is correct
     *
     * @param toParse the string to parse
     * @return the parsed Duration
     */
    Duration parseDurationString(String toParse);
}
