package at.pegelhub.connector.ftp;

import at.pegelhub.connector.ftp.config.FtpConnectorConfig;
import at.pegelhub.connector.ftp.fileparsing.Entry;
import at.pegelhub.connector.ftp.fileparsing.Parser;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.model.Measurement;
import org.apache.commons.net.ftp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FtpImportJob implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(FtpImportJob.class);
    private final Set<FtpFileKey> processedFiles = new HashSet<>();
    private final Instant lookBackStart;
    private final FTPClient ftp;
    private final FtpConnectorConfig config;
    private final PegelHubClient communicator;
    private final Parser parser;

    public FtpImportJob(FTPClient ftp, FtpConnectorConfig config, PegelHubClient communicator, Parser parser) {
        this.ftp = ftp;
        this.config = config;
        this.communicator = communicator;
        this.parser = parser;
        this.lookBackStart = Instant.now().minus(config.pollInterval());
    }

    /** Imports recent, unprocessed files from the configured FTP directory into Core. */
    @Override
    public void run(){
        try {
            ftp.connect(
                    config.server().host(),
                    config.server().port());
            FTPClientConfig conf = new FTPClientConfig(FTPClientConfig.SYST_UNIX);
            conf.setUnparseableEntries(true);
            ftp.configure(conf);
            ftp.setDefaultPort(21);
            int replyCode = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                throw new RuntimeException("FTP couldn't connect!");
            }
            if (!ftp.login(
                    config.server().authentication().username(),
                    config.server().authentication().password())) {
                ftp.disconnect();
                throw new RuntimeException("Unable to login!");
            }
        } catch (IOException e) {
            LOG.error("Couldn't login to FTP!", e);
            return;
        }

        try {
            ftp.enterLocalPassiveMode();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            FTPFile[] files;
            try {
                LOG.debug("Listing files under {}", config.source().directory());
                files = ftp.listFiles(config.source().directory(), this::shouldProcess);
            } catch (IOException e) {
                LOG.error("Can't list files!", e);
                return;
            }

            List<ParsedFile> parsedFiles = Arrays.stream(files)
                    .map(this::parseFile)
                    .flatMap(Optional::stream)
                    .toList();
            List<Measurement> measurements = parsedFiles.stream()
                    .flatMap(file -> file.entries().stream())
                    .flatMap(this::convertEntryToMeasurementStream)
                    .collect(Collectors.toList());

            if (!measurements.isEmpty()) {
                communicator.sendMeasurements(measurements);
            }
            parsedFiles.stream().map(ParsedFile::key).forEach(processedFiles::add);
        } catch (Exception e) {
            LOG.error("Unhandled Exception was thrown!", e);
        } finally {
            try {
                ftp.logout();
            } catch (IOException e) {
                LOG.error("Couldn't logout of FTP!", e);
            }
            try {
                ftp.disconnect();
            } catch (IOException e) {
                LOG.error("Couldn't disconnect!", e);
            }
        }
    }

    private boolean shouldProcess(FTPFile file) {
        return file != null
                && file.isFile()
                && file.getName() != null
                && file.getTimestampInstant() != null
                && file.getName().endsWith(parser.getType().fileSuffix)
                && file.getTimestampInstant().isAfter(lookBackStart)
                && !processedFiles.contains(FtpFileKey.from(file));
    }

    private Optional<ParsedFile> parseFile(FTPFile file) {
        String remoteDirectory = config.source().directory();
        final String formatString = remoteDirectory.endsWith("/") ? "%s%s" : "%s/%s";
        final String fileLocation = String.format(formatString, remoteDirectory, file.getName());
        LOG.debug("Parsing FTP file {} from {}", file.getName(), fileLocation);
        InputStream fileStream = getFileInputStream(fileLocation);

        if (fileStream == null) {
            return Optional.empty();
        }

        List<Entry> entries;
        try (fileStream; Stream<Entry> parsedEntries = parser.parse(fileStream)) {
            entries = parsedEntries.toList();
        } catch (IOException e) {
            LOG.error("Error while reading file!", e);
            return Optional.empty();
        }

        try {
            if (!ftp.completePendingCommand()) {
                LOG.error("FTP transfer did not complete for {}.", fileLocation);
                return Optional.empty();
            }
        } catch (IOException e) {
            LOG.error("Could not complete FTP transfer for {}.", fileLocation, e);
            return Optional.empty();
        }

        return Optional.of(new ParsedFile(FtpFileKey.from(file), entries));
    }

    private InputStream getFileInputStream(String location) {
        InputStream fileStream;
        try {
            LOG.debug("Opening FTP stream for {}", location);
            fileStream = ftp.retrieveFileStream(location);
            if (fileStream == null) {
                LOG.error("Couldn't open filestream for \"{}\". No exception was thrown, but filestream is null.", location);
                return null;
            }
        } catch (IOException e) {
            LOG.error(String.format("IOException was thrown while opening filestream for \"%s\"!", location), e);
            return null;
        }
        return fileStream;
    }

    private Stream<Measurement> convertEntryToMeasurementStream(Entry e) {
        FtpImportMapping mapping = config.mapping();
        String parameter = e.getInfos().get("parameter");
        if (mapping.sourceParameter() != null
                && !mapping.sourceParameter().equalsIgnoreCase(parameter)) {
            return Stream.of();
        }
        if (!isSupportedPhysicalUnit(parameter, e.getInfos().get("unit"))) {
            LOG.warn("Ignoring FTP entry with unsupported physical unit: parameter={}, unit={}", parameter, e.getInfos().get("unit"));
            return Stream.of();
        }

        return e.getValues().entrySet().stream().map(value -> {
            if (!Util.canParseDouble(value.getValue()) || !Util.canParseDouble(e.getInfos().get("location"))) {
                return null;
            }

            if (mapping.sourceStationId() >= 0
                    && Integer.parseInt(e.getInfos().get("location")) != mapping.sourceStationId()) {
                return null;
            }

            double parsedValue = Double.parseDouble(value.getValue());
            double canonicalValue = canonicalValue(parameter, e.getInfos().get("unit"), parsedValue);
            return new Measurement(
                    mapping.targetTimeSeriesId(),
                    value.getKey().toInstant(),
                    canonicalValue);
        }).filter(Objects::nonNull);
    }

    private boolean isSupportedPhysicalUnit(String parameter, String unit) {
        if (parameter == null || unit == null) {
            return false;
        }
        String normalized = normalizeUnit(unit);
        return switch (parameter.trim().toLowerCase(Locale.ROOT)) {
            case "wasserstandabs" -> normalized.equals("mua") || normalized.equals("müa");
            case "wasserstand" -> normalized.equals("cm") || normalized.equals("mm");
            case "abfluss" -> normalized.equals("m3/s") || normalized.equals("l/s");
            case "wtemperatur" -> normalized.equals("°c") || normalized.equals("c") || normalized.equals("cel");
            default -> false;
        };
    }

    private double canonicalValue(String parameter, String unit, double value) {
        String normalized = normalizeUnit(unit);
        if ("wasserstand".equalsIgnoreCase(parameter) && "mm".equals(normalized)) {
            return value / 10d;
        }
        if ("abfluss".equalsIgnoreCase(parameter) && "l/s".equals(normalized)) {
            return value / 1000d;
        }
        return value;
    }

    private String normalizeUnit(String unit) {
        return unit.trim().toLowerCase(Locale.ROOT)
                .replace("³", "3")
                .replace(" ", "")
                .replace(".", "");
    }

    private record FtpFileKey(String name, Instant modifiedAt) {
        private static FtpFileKey from(FTPFile file) {
            return new FtpFileKey(file.getName(), file.getTimestampInstant());
        }
    }

    private record ParsedFile(FtpFileKey key, List<Entry> entries) {
    }
}
