package at.pegelhub.connector.ftp.test;

import at.pegelhub.connector.ftp.FtpImportMapping;
import at.pegelhub.connector.ftp.FtpImportJob;
import at.pegelhub.connector.ftp.config.FtpAuthentication;
import at.pegelhub.connector.ftp.config.FtpConnectorConfig;
import at.pegelhub.connector.ftp.config.FtpServer;
import at.pegelhub.connector.ftp.config.FtpSource;
import at.pegelhub.connector.ftp.fileparsing.Entry;
import at.pegelhub.connector.ftp.fileparsing.Parser;
import at.pegelhub.connector.ftp.fileparsing.ParserFactory;
import at.pegelhub.connector.ftp.fileparsing.ParserType;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.config.CoreAuthentication;
import at.pegelhub.lib.config.CoreConnection;
import at.pegelhub.lib.model.Measurement;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.jupiter.api.*;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class FtpImportJobTest {
    private FTPClient client;
    private PegelHubClient comm;
    private FtpConnectorConfig config;
    private static final UUID TIME_SERIES_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final int STATION_ID = 10001033;

    @BeforeEach
    public void setup() throws Exception {
        client = new FTPClient();
        comm = mock(PegelHubClient.class);
        config = buildConfig(ParserType.ASC, STATION_ID, null, 21);
    }

    @Test
    public void nothingIsSentWhenFTPThrowsIOException() throws IOException {
        var mockClient = mock(FTPClient.class);
        when(mockClient.listFiles(any(), any())).thenThrow(new IOException());
        when(mockClient.getReplyCode()).thenReturn(200);
        when(mockClient.login(any(), any())).thenReturn(true);
        var mockParser = mock(Parser.class);
        var newTask = new FtpImportJob(mockClient, config, comm, mockParser);

        newTask.run();

        verify(comm, never()).sendMeasurements(any());
    }

    @Test
    public void nothingIsSentWhenFTPFileStreamThrows() throws IOException {
        var mockClient = mock(FTPClient.class);
        when(mockClient.listFiles(any(), any())).thenReturn(new FTPFile[]{new FTPFile()});
        when(mockClient.retrieveFileStream(any())).thenThrow(new IOException());
        when(mockClient.getReplyCode()).thenReturn(200);
        when(mockClient.login(any(), any())).thenReturn(true);
        var mockParser = mock(Parser.class);
        var newTask = new FtpImportJob(mockClient, config, comm, mockParser);

        newTask.run();

        verify(comm, never()).sendMeasurements(any());
    }

    @Test
    public void onlyConfiguredParameterIsSent() throws Exception {
        var mockClient = mock(FTPClient.class);
        var file = new FTPFile();
        file.setName("values.zrxp");
        when(mockClient.listFiles(any(), any())).thenReturn(new FTPFile[]{file});
        when(mockClient.retrieveFileStream(any())).thenReturn(InputStream.nullInputStream());
        when(mockClient.completePendingCommand()).thenReturn(true);
        when(mockClient.getReplyCode()).thenReturn(200);
        when(mockClient.login(any(), any())).thenReturn(true);
        var parser = mock(Parser.class);
        var ignoredEntry = entry("10001033", "Abfluss", 118.8);
        var matchingEntry = entry("10001033", "WasserstandAbs", 157.3);
        when(parser.parse(any())).thenReturn(List.of(ignoredEntry, matchingEntry).stream());
        var config = buildConfig(ParserType.ZRXP, STATION_ID, "WasserstandAbs", 21);

        new FtpImportJob(mockClient, config, comm, parser).run();

        verify(comm).sendMeasurements(argThat(measurements -> {
            assertEquals(1, measurements.size());
            Measurement measurement = measurements.getFirst();
            assertEquals(TIME_SERIES_ID, measurement.getTimeSeriesId());
            assertEquals(157.3, measurement.getValue());
            return true;
        }));
    }

    private static Entry entry(String location, String parameter, double value) {
        var entry = mock(Entry.class);
        when(entry.getInfos()).thenReturn(Map.of("location", location, "parameter", parameter));
        when(entry.getValues()).thenReturn(Map.of(Date.from(Instant.parse("2026-06-25T07:00:00Z")), Double.toString(value)));
        return entry;
    }

    @Nested
    public class TestWithRunningServer {
        private FakeFtpServer server;
        private FtpImportJob task;

        private static final Logger LOG = LoggerFactory.getLogger(TestWithRunningServer.class);

        @BeforeEach
        public void setup(TestInfo t) throws Exception {
            String[] tags = t.getTags().toArray(new String[0]);
            FileSystem fs = new UnixFakeFileSystem();
            fs.add(new FileEntry("/TestFile.asc", Utils.getResource(tags[0])));
            server = new FakeFtpServer();
            server.addUserAccount(new UserAccount("user", "password", "/"));
            server.setFileSystem(fs);
            server.setServerControlPort(1025);
            server.start();
            var newConfig = buildConfig(ParserType.ASC, STATION_ID, null, 1025);
            var parser = ParserFactory.getParser(newConfig.source().parserType());
            task = new FtpImportJob(client, newConfig, comm, parser);
        }

        @AfterEach
        public void teardown() {
            server.stop();
        }

        @Test
        @Tag("SingleEntry.asc")
        public void ftpTaskLoadsASCFileAndSendsEntriesToCommunicator() {
            task.run();
            verify(comm, atLeastOnce()).sendMeasurements(argThat(measurements -> {
                Measurement measurement = measurements.getFirst();
                assertEquals(TIME_SERIES_ID, measurement.getTimeSeriesId());
                assertEquals(289.0, measurement.getValue());
                return true;
            }));
        }

        @Test
        @Tag("MultipleEntries.asc")
        public void ftpTaskLoadsFileWithMultipleEntriesAndSendsEntriesToCommunicator() {
            task.run();

            verify(comm, atLeastOnce()).sendMeasurements(argThat(measurements -> {
                Measurement measurement = measurements.getFirst();
                assertEquals(TIME_SERIES_ID, measurement.getTimeSeriesId());
                return true;
            }));
        }

        @Test
        @Tag("SingleEntry.asc")
        public void ftpTaskDoesNotProcessTheSameFileTwice() {
            task.run();
            task.run();

            verify(comm, times(1)).sendMeasurements(anyList());
        }

        @Test
        @Tag("GeneralError.asc")
        public void ftpTaskDoesNotThrowOnParseError() {
            assertDoesNotThrow(task::run);
        }
    }

    private static FtpConnectorConfig buildConfig(
            ParserType parserType,
            int stationId,
            String parameter,
            int ftpPort
    ) throws Exception {
        return new FtpConnectorConfig(
                new CoreConnection(
                        URI.create("http://localhost:8080/").toURL(),
                        new CoreAuthentication("http://keycloak.local/token", "connector", "secret")),
                new FtpServer(
                        "localhost",
                        ftpPort,
                        new FtpAuthentication("user", "password")),
                new FtpSource("/", parserType),
                Duration.ofHours(2),
                new FtpImportMapping(stationId, parameter, TIME_SERIES_ID));
    }
}
