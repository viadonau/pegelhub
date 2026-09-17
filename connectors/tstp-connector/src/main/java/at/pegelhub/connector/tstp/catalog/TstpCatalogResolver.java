package at.pegelhub.connector.tstp.catalog;

import at.pegelhub.connector.tstp.TstpParameter;
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
    private final Map<CatalogKey, CachedZrid> catalogEntries = new HashMap<>();

    public TstpCatalogResolver(TstpClient client) {
        this(client, Clock.systemUTC());
    }

    TstpCatalogResolver(TstpClient client, Clock clock) {
        this.client = client;
        this.clock = clock;
    }

    public synchronized String resolveZrid(int stationId, TstpParameter parameter, String unit) {
        CatalogKey key = new CatalogKey(stationId, parameter);
        CachedZrid cached = catalogEntries.get(key);
        Instant now = clock.instant();

        if (cached == null || !cached.loadedAt().plus(CACHE_DURATION).isAfter(now)) {
            XmlQueryTsAttribut entry = requireEntry(client.readCatalog(stationId, parameter), key);
            requireUnit(unit, entry.getEinheit(), key);
            cached = new CachedZrid(entry.getZrid(), entry.getEinheit(), now);
            catalogEntries.put(key, cached);
        }

        requireUnit(unit, cached.unit(), key);
        return cached.zrid();
    }

    private XmlQueryTsAttribut requireEntry(XmlQueryResponse catalog, CatalogKey key) {
        if (catalog == null) {
            throw new IllegalStateException("TSTP did not return a catalog for " + key);
        }
        List<XmlQueryTsAttribut> entries = catalog.getDef();
        if (entries == null || entries.size() != 1) {
            throw new IllegalStateException("TSTP catalog must contain exactly one main series for " + key);
        }

        XmlQueryTsAttribut entry = entries.getFirst();
        String zrid = entry == null ? null : entry.getZrid();
        if (zrid == null || zrid.isBlank()) {
            throw new IllegalStateException("TSTP catalog did not contain a ZRID for " + key);
        }
        if (!Integer.toString(key.stationId()).equals(entry.getOrt())
                || !key.parameter().value().equals(entry.getParameter())) {
            throw new IllegalStateException("TSTP catalog returned a different station or parameter for " + key);
        }
        if (!"T".equalsIgnoreCase(entry.getHauptReihe()) && !"true".equalsIgnoreCase(entry.getHauptReihe())) {
            throw new IllegalStateException("TSTP catalog did not confirm a main series for " + key);
        }
        return entry;
    }

    private void requireUnit(String expected, String actual, CatalogKey key) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException("TSTP catalog unit mismatch for " + key
                    + ": expected " + expected + ", got " + actual);
        }
    }

    private record CatalogKey(int stationId, TstpParameter parameter) {}

    private record CachedZrid(
            String zrid,
            String unit,
            Instant loadedAt
    ) {}
}
