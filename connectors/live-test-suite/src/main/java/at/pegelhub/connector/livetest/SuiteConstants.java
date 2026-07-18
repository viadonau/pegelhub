package at.pegelhub.connector.livetest;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

final class SuiteConstants {
    static final int ADMIN_PORT = 19000;
    static final int LOCAL_CORE_PORT = 18080;
    static final int EXTERNAL_CORE_PORT = 18081;
    static final int LOCAL_KEYCLOAK_PORT = 18082;
    static final int EXTERNAL_KEYCLOAK_PORT = 18083;
    static final int TSTP_PORT = 18030;
    static final int FTP_PORT = 2121;
    static final int IEC_PORT = 2404;
    static final int IEC_COMMON_ADDRESS = 1;

    static final String TOKEN = "live-token";

    static final int FTP_STATION_ID = 10001033;
    static final int TSTP_READER_STATION_ID = 10002001;
    static final int TSTP_WRITER_STATION_ID = 10002002;
    static final String TSTP_READER_ZRID = "LIVE_READER_ZRID";
    static final String TSTP_WRITER_ZRID = "LIVE_WRITER_ZRID";

    static final int IEC_EXTERNAL_TO_CORE_IOA = 66049;
    static final int IEC_CORE_TO_EXTERNAL_IOA = 66050;
    static final int IEC_UNMAPPED_IOA = 66999;

    static final UUID FTP_ASC_TS = UUID.fromString("11111111-1111-1111-1111-111111111101");
    static final UUID FTP_ZRXP_TS = UUID.fromString("11111111-1111-1111-1111-111111111102");
    static final UUID TSTP_READER_TS = UUID.fromString("11111111-1111-1111-1111-111111111201");
    static final UUID TSTP_WRITER_TS = UUID.fromString("11111111-1111-1111-1111-111111111202");
    static final UUID IEC_EXTERNAL_TO_CORE_TS = UUID.fromString("11111111-1111-1111-1111-111111111301");
    static final UUID IEC_CORE_TO_EXTERNAL_TS = UUID.fromString("11111111-1111-1111-1111-111111111302");
    static final UUID ICC_LOCAL_SOURCE_TS = UUID.fromString("11111111-1111-1111-1111-111111111401");
    static final UUID ICC_EXTERNAL_TARGET_TS = UUID.fromString("11111111-1111-1111-1111-111111111402");
    static final UUID ICC_EXTERNAL_SOURCE_TS = UUID.fromString("11111111-1111-1111-1111-111111111403");
    static final UUID ICC_LOCAL_TARGET_TS = UUID.fromString("11111111-1111-1111-1111-111111111404");

    static final MeasurementRecord FTP_ASC_MEASUREMENT =
            new MeasurementRecord(FTP_ASC_TS, Instant.parse("2026-06-25T07:00:00Z"), 289.0);
    static final MeasurementRecord FTP_ZRXP_MEASUREMENT =
            new MeasurementRecord(FTP_ZRXP_TS, Instant.parse("2026-06-25T07:15:00Z"), 157.3);
    static final Set<UUID> FTP_WRITE_IDS = Set.of(FTP_ASC_TS, FTP_ZRXP_TS);
    static final Set<UUID> TSTP_WRITE_IDS = Set.of(TSTP_READER_TS);
    static final Set<UUID> IEC_WRITE_IDS = Set.of(IEC_EXTERNAL_TO_CORE_TS);
    static final Set<UUID> ICC_WRITE_IDS = Set.of(ICC_EXTERNAL_TARGET_TS, ICC_LOCAL_TARGET_TS);

    private SuiteConstants() {
    }
}
