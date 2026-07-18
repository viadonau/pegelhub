package at.pegelhub.connector.tstp.service.impl;

import at.pegelhub.connector.tstp.communication.TstpCommunicator;
import at.pegelhub.connector.tstp.service.TstpCatalogService;
import at.pegelhub.connector.tstp.service.model.XmlQueryResponse;
import at.pegelhub.connector.tstp.service.model.XmlQueryTsAttribut;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor
public class TstpCatalogServiceImpl implements TstpCatalogService {
    private static final Logger LOG = LoggerFactory.getLogger(TstpCatalogServiceImpl.class);
    private Instant latestRefresh;
    private XmlQueryResponse catalog;
    private TstpCommunicator communicator;
    private int dbms;

    public TstpCatalogServiceImpl(TstpCommunicator communicator, int dbms) {
        this.communicator = communicator;
        this.dbms = dbms;
    }

    @Override
    public String getZrid() {
        LOG.info("getting ZRID");
        ensureCatalog();
        String zrid = firstCatalogEntry().getZrid();
        if (zrid == null || zrid.isBlank()) {
            throw new IllegalStateException("TSTP catalog did not contain a ZRID for station " + dbms);
        }
        return zrid;
    }

    @Override
    public Instant getMaxFocusEnd() {
        ensureCatalog();
        String maxFocusEnd = firstCatalogEntry().getMaxFocusEnd();
        if (maxFocusEnd == null || maxFocusEnd.isBlank()) {
            throw new IllegalStateException("TSTP catalog did not contain MAXFOCUS-End for station " + dbms);
        }
        return Instant.parse(maxFocusEnd);
    }

    private void ensureCatalog() {
        if (!isCatalogInSync()) {
            LOG.info("Catalog out of sync");
            refreshCatalog();
        }
    }

    private void refreshCatalog() {
        Optional<XmlQueryResponse> loadedCatalog = communicator.getCatalog(dbms);
        catalog = loadedCatalog.orElseThrow(() ->
                new IllegalStateException("TSTP catalog is unavailable for station " + dbms));
        this.latestRefresh = Instant.now();
    }

    private boolean isCatalogInSync() {
        return latestRefresh != null && latestRefresh.isAfter(Instant.now().minus(24, ChronoUnit.HOURS));
    }

    private XmlQueryTsAttribut firstCatalogEntry() {
        List<XmlQueryTsAttribut> entries = catalog.getDef();
        if (entries == null || entries.isEmpty()) {
            throw new IllegalStateException("TSTP catalog did not contain entries for station " + dbms);
        }
        return entries.getFirst();
    }
}
