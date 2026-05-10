package at.pegelhub.connector.tstp.service;

import java.time.Instant;

/**
 * Handles the logic for refreshing the catalog and always receiving the up to date ZRIDs
 */
public interface TstpCatalogService {
    /**
     * Get the up to date ZRID
     *
     * @return the requested ZRID
     */
    String getZrid();

    /**
     * Get the MAXFOCUS-End from the catalog
     *
     * @return the MAXFOCUS-End date
     */
    Instant getMaxFocusEnd();
}
