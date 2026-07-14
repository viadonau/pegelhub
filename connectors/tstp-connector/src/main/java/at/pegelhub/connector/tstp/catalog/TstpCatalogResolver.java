package at.pegelhub.connector.tstp.catalog;

import at.pegelhub.connector.tstp.client.TstpClient;
import at.pegelhub.connector.tstp.service.model.XmlQueryResponse;
import at.pegelhub.connector.tstp.service.model.XmlQueryTsAttribut;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TstpCatalogResolver {
    private static final Duration CACHE_DURATION = Duration.ofHours(24);

    private final TstpClient client;
    private final Clock clock;
    private final Map<Integer, CachedCatalog> catalogs = new HashMap<>();

    public TstpCatalogResolver(TstpClient client) {
        this(client, Clock.systemUTC());
    }

    TstpCatalogResolver(TstpClient client, Clock clock) {
        this.client = client;
        this.clock = clock;
    }

    public synchronized String resolveZrid(int stationId) {
        CachedCatalog cached = catalogs.get(stationId);
        Instant now = clock.instant();
        if (cached == null || !cached.loadedAt().plus(CACHE_DURATION).isAfter(now)) {
            cached = new CachedCatalog(client.readCatalog(stationId), now);
            catalogs.put(stationId, cached);
        }
        List<XmlQueryTsAttribut> entries = cached.catalog().getDef();
        if (entries == null || entries.isEmpty()) {
            throw new IllegalStateException("TSTP catalog did not contain entries for station " + stationId);
        }
        String zrid = entries.getFirst().getZrid();
        if (zrid == null || zrid.isBlank()) {
            throw new IllegalStateException("TSTP catalog did not contain a ZRID for station " + stationId);
        }
        return zrid;
    }

    private record CachedCatalog(XmlQueryResponse catalog, Instant loadedAt) {
    }
}
