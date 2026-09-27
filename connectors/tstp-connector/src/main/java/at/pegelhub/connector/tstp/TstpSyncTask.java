package at.pegelhub.connector.tstp;

import java.time.Instant;

interface TstpSyncTask {
    TstpMapping mapping();

    void synchronize(Instant cycleUntil);
}
