package at.pegelhub.connector.tstp.service.impl;

import at.pegelhub.connector.tstp.communication.TstpCommunicator;
import at.pegelhub.connector.tstp.service.model.XmlQueryResponse;
import at.pegelhub.connector.tstp.service.TstpCatalogService;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

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
        refreshCatalog();
    }

    @Override
    public String getZrid() {
        LOG.info("getting ZRID");
        if (!isCatalogInSync()) {
            LOG.info("Catalog out of sync");
            refreshCatalog();
        }
        if(this.catalog == null){
            LOG.info("Catalog is null - there was an error");
            return "";
        }
        return catalog.getDef().get(0).getZrid();
    }

    @Override
    public Instant getMaxFocusEnd() {
        if (this.catalog == null) {
           return null;
        }
        return Instant.parse(catalog.getDef().get(0).getMaxFocusEnd());
    }

    private void refreshCatalog() {
        catalog = communicator.getCatalog(dbms);
        this.latestRefresh = Instant.now();
    }

    private boolean isCatalogInSync() {
        return latestRefresh.isAfter(Instant.now().minus(24, ChronoUnit.HOURS));
    }
}
