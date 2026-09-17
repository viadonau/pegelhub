package at.pegelhub.connector.tstp.codec;

import at.pegelhub.connector.tstp.service.model.XmlQueryResponse;
import at.pegelhub.connector.tstp.service.model.XmlTsData;
import at.pegelhub.connector.tstp.service.model.XmlTsDefinition;
import at.pegelhub.connector.tstp.service.model.XmlTsResponse;
import at.pegelhub.lib.model.Measurement;
import at.pegelhub.connector.tstp.config.TstpWriteFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.glassfish.jaxb.core.marshaller.CharacterEscapeHandler;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.Base64;
import java.util.List;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Collectors;

public final class TstpXmlCodec {
    private static final Logger LOG = LoggerFactory.getLogger(TstpXmlCodec.class);

    private final TstpBinaryCodec binaryCodec;
    private final DateTimeFormatter timeFormat;
    private final TstpWriteFormat writeFormat;

    public TstpXmlCodec(TstpBinaryCodec binaryCodec, TstpWriteFormat writeFormat) {
        this.binaryCodec = binaryCodec;
        this.writeFormat = java.util.Objects.requireNonNull(writeFormat, "writeFormat");
        this.timeFormat = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT)
                .withZone(binaryCodec.timeOffset());
    }

    public List<Measurement> parseMeasurements(byte[] responseBody, String unit) {
        XmlTsData responseObject = unmarshalXmlTsData(responseBody);
        if (responseObject.getDef() == null || !unit.equals(responseObject.getDef().getEinheit())) {
            throw new IllegalArgumentException("TSTP measurement response did not confirm unit " + unit);
        }
        LOG.debug("unmarshalled get response");

        return parseXmlTsDataToMeasurementList(responseObject);
    }

    public XmlQueryResponse parseCatalog(byte[] xmlCatalog) {
        return unmarshalXmlCatalog(xmlCatalog);
    }

    public XmlTsResponse parseWriteResponse(byte[] xml) {
        return unmarshalXmlTsResponse(xml);
    }

    public String writeRequest(List<Measurement> measurements, String unit) {
        if (measurements.isEmpty()) {
            throw new IllegalArgumentException("TSTP write requires at least one measurement");
        }
        for (Measurement measurement : measurements) {
            Double value = measurement.getValue();
            if (measurement.getObservedAt() == null || value == null || !Double.isFinite(value)
                    || !Float.isFinite(value.floatValue()) || Float.floatToIntBits(value.floatValue()) == 0x7df0bdc2) {
                throw new IllegalArgumentException("TSTP write requires finite measurements, not gap markers");
            }
        }
        String data;
        String length;
        if (writeFormat == TstpWriteFormat.BINARY) {
            byte[] bytes = binaryCodec.encode(measurements);
            data = Base64.getMimeEncoder(60, new byte[]{'\n'}).encodeToString(bytes);
            length = String.valueOf(bytes.length);
        } else {
            // TSTP section 6.2: LEN=0 selects text pairs; avoids observed binary PUT quantization.
            data = measurements.stream().map(measurement -> timeFormat.format(measurement.getObservedAt())
                    + " " + Double.toString(measurement.getValue())).collect(Collectors.joining("\n"));
            length = "0";
        }

        XmlTsDefinition xmlTsDef = new XmlTsDefinition(
                "Z",
                "Nein",
                "K",
                unit,
                length,
                String.valueOf(measurements.size())
        );
        XmlTsData xmlTsData = new XmlTsData("1", xmlTsDef, data);

        return marshallXmlTsData(xmlTsData);
    }

    private String marshallXmlTsData(XmlTsData tsData) {
        try {
            JAXBContext jc = JAXBContext.newInstance(XmlTsData.class);
            StringWriter sw = new StringWriter();

            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(CharacterEscapeHandler.class.getName(),
                    (CharacterEscapeHandler) (ac, i, j, flag, writer) -> writer.write(ac, i, j));
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "ISO-8859-1");

            marshaller.marshal(tsData, sw);
            return sw.toString();
        } catch (JAXBException e) {
            throw new IllegalArgumentException("Could not encode TSTP measurement XML", e);
        }
    }

    private XmlTsResponse unmarshalXmlTsResponse(byte[] xml) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(XmlTsResponse.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            ByteArrayInputStream reader = new ByteArrayInputStream(xml);

            return (XmlTsResponse) unmarshaller.unmarshal(reader);
        } catch (JAXBException e) {
            throw new IllegalArgumentException("Could not decode TSTP write response", e);
        }
    }

    private XmlQueryResponse unmarshalXmlCatalog(byte[] xmlCatalog) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(XmlQueryResponse.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            ByteArrayInputStream reader = new ByteArrayInputStream(xmlCatalog);

            return (XmlQueryResponse) unmarshaller.unmarshal(reader);
        } catch (JAXBException e) {
            throw new IllegalArgumentException("Could not decode TSTP catalog XML", e);
        }
    }

    private List<Measurement> parseXmlTsDataToMeasurementList(XmlTsData data) {
        String rawMeasurements = data.getData().replace("\n", "");
        byte[] decoded = Base64.getDecoder().decode(rawMeasurements);

        return binaryCodec.decode(decoded);
    }

    private XmlTsData unmarshalXmlTsData(byte[] xml) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(XmlTsData.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            ByteArrayInputStream reader = new ByteArrayInputStream(xml);

            return (XmlTsData) unmarshaller.unmarshal(reader);
        } catch (JAXBException e) {
            throw new IllegalArgumentException("Could not decode TSTP measurement XML", e);
        }
    }

}
