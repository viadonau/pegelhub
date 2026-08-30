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
    private final Map<Integer, CachedZrid> catalogEntries = new HashMap<>();

    public TstpCatalogResolver(TstpClient client) {
        this(client, Clock.systemUTC());
    }

    TstpCatalogResolver(TstpClient client, Clock clock) {
        this.client = client;
        this.clock = clock;
    }

    public synchronized String resolveZrid(int stationId) {
        CachedZrid cached = catalogEntries.get(stationId);
        Instant now = clock.instant();

        if (cached == null || !cached.loadedAt().plus(CACHE_DURATION).isAfter(now)) {
            String zrid = requireZrid(client.readCatalog(stationId), stationId);
            cached = new CachedZrid(zrid, now);
            catalogEntries.put(stationId, cached);
        }

        return cached.zrid();
    }

    private String requireZrid(XmlQueryResponse catalog, int stationId) {
        if (catalog == null) {
            throw new IllegalStateException("TSTP did not return a catalog for station " + stationId);
        }
        List<XmlQueryTsAttribut> entries = catalog.getDef();
        if (entries == null || entries.isEmpty()) {
            throw new IllegalStateException("TSTP catalog did not contain entries for station " + stationId);
        }

        XmlQueryTsAttribut firstEntry = entries.getFirst();
        String zrid = firstEntry == null ? null : firstEntry.getZrid();
        if (zrid == null || zrid.isBlank()) {
            throw new IllegalStateException("TSTP catalog did not contain a ZRID for station " + stationId);
        }

        return zrid;
    }

    private record CachedZrid(
            String zrid,
            Instant loadedAt
    ) {}
}
