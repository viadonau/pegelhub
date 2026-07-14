package at.pegelhub.connector.ftp.test;

import at.pegelhub.connector.ftp.FtpConnectorModule;
import at.pegelhub.connector.ftp.FtpImportJob;
import at.pegelhub.lib.CoreConnection;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.PegelHubClientFactory;
import at.pegelhub.lib.runtime.ConnectorApplication;
import org.apache.commons.net.ftp.FTPClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URL;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class FtpConnectorTest {
    @TempDir
    Path configDir;

    @BeforeEach
    public void setup() throws Exception {
        Files.writeString(configDir.resolve("connector.yaml"), """
                core:
                  baseUrl: "http://127.0.0.1:8081/"
                keycloak:
                  tokenUrl: "http://keycloak.local/token"
                  clientId: "connector"
                  clientSecret: "secret"
                schedule:
                  delay: "2h"
                ftp:
                  address: "77.244.244.162"
                  port: 21
                  user: "web47ftppegelsend"
                  password: "noHeshEkJernaw7"
                  path: "/"
                  parserType: "asc"
                """);
        Files.createDirectories(configDir.resolve("mappings"));
        Files.writeString(configDir.resolve("mappings/station.yaml"), """
                timeSeriesId: "11111111-1111-1111-1111-111111111111"
                stationId: 1
                direction: "external-to-core"
                """);
    }

    @Test
    public void createsPegelHubClientWithCorrectURL() throws Exception {
        URL expectedCoreUrl = URI.create("http://127.0.0.1:8081/").toURL();
        PegelHubClient client = mock(PegelHubClient.class);
        try (var ftpClientMock = mockConstruction(FTPClient.class, (mock, context) -> {
                when(mock.getReplyCode()).thenReturn(200);
                when(mock.login("user", "pass")).thenReturn(true);
            });
             var taskMock = mockConstruction(FtpImportJob.class)) {
            new FtpConnectorModule().define(at.pegelhub.lib.runtime.ConnectorBootstrap.forDirectory(
                    configDir, connection -> {
                        org.junit.jupiter.api.Assertions.assertEquals(expectedCoreUrl, connection.baseUrl());
                        org.junit.jupiter.api.Assertions.assertEquals("connector", connection.credentials().clientId());
                        return client;
                    }));
        }
    }

    @Test
    public void runtimeStartsScheduledTask() throws Exception {
        try (var ftpClientMock = mockConstruction(FTPClient.class, (mock, context) -> {
                 when(mock.getReplyCode()).thenReturn(200);
                 when(mock.login("user", "pass")).thenReturn(true);
             });
             var taskMock = mockConstruction(FtpImportJob.class)) {
            try (var con = ConnectorApplication.start(new String[]{configDir.toString()}, new FtpConnectorModule(),
                    connection -> mock(PegelHubClient.class))) {
                verify(taskMock.constructed().get(0), timeout(1000).atLeastOnce()).run();
            }
        }
    }

    @Test
    public void allMembersAreDisposedOnClose() throws Exception {
        try (var ftpClientMock = mockConstruction(FTPClient.class, (mock, context) -> {
                 when(mock.getReplyCode()).thenReturn(200);
                 when(mock.login("user", "pass")).thenReturn(true);
             });
             var taskMock = mockConstruction(FtpImportJob.class)) {
            var pegelCommMock = mock(PegelHubClient.class);
            try (var con = ConnectorApplication.start(new String[]{configDir.toString()}, new FtpConnectorModule(),
                    connection -> pegelCommMock)) {
                con.close();
            }

            verify(pegelCommMock, times(1)).close();
            verify(ftpClientMock.constructed().get(0), times(1)).disconnect();
        }
    }
}
