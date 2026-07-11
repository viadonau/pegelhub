package at.pegelhub.connector.livetest;

import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;

final class FakeFtpService implements AutoCloseable {
    private final FakeFtpServer server = new FakeFtpServer();
    private final Path fixturesDir;

    FakeFtpService(Path fixturesDir) {
        this.fixturesDir = fixturesDir;
    }

    void start() throws IOException {
        FileSystem fs = new UnixFakeFileSystem();
        fs.add(new DirectoryEntry("/asc"));
        fs.add(new DirectoryEntry("/zrxp"));
        fs.add(file("/asc/live.asc", "ftp/live.asc", Instant.now().plusSeconds(3600)));
        fs.add(file("/asc/stale.asc", "ftp/stale.asc", Instant.parse("2020-01-01T00:00:00Z")));
        fs.add(file("/asc/wrong.txt", "ftp/wrong-suffix.asc", Instant.now().plusSeconds(3600)));
        fs.add(file("/zrxp/live.zrxp", "ftp/live.zrxp", Instant.now().plusSeconds(3600)));
        fs.add(file("/zrxp/stale.zrxp", "ftp/stale.zrxp", Instant.parse("2020-01-01T00:00:00Z")));
        fs.add(file("/zrxp/wrong.asc", "ftp/live.zrxp", Instant.now().plusSeconds(3600)));

        server.addUserAccount(new UserAccount("user", "password", "/"));
        server.setFileSystem(fs);
        server.setServerControlPort(SuiteConstants.FTP_PORT);
        server.start();
    }

    private FileEntry file(String path, String fixture, Instant lastModified) throws IOException {
        FileEntry entry = new FileEntry(path, Files.readString(fixturesDir.resolve(fixture)));
        entry.setLastModified(Date.from(lastModified));
        return entry;
    }

    @Override
    public void close() {
        server.stop();
    }
}
