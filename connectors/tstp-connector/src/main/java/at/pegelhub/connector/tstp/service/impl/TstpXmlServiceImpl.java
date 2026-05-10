package at.pegelhub.connector.tstp.service.impl;

import at.pegelhub.connector.tstp.service.TstpBinaryService;
import at.pegelhub.connector.tstp.service.TstpXmlService;
import at.pegelhub.connector.tstp.service.model.XmlTsData;
import at.pegelhub.connector.tstp.service.model.XmlQueryResponse;
import at.pegelhub.connector.tstp.service.model.XmlTsDefinition;
import at.pegelhub.connector.tstp.service.model.XmlTsResponse;
import at.pegelhub.lib.model.Measurement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.glassfish.jaxb.core.marshaller.CharacterEscapeHandler;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

public class TstpXmlServiceImpl implements TstpXmlService {
    private static final Logger LOG = LoggerFactory.getLogger(TstpXmlServiceImpl.class);
    private final TstpBinaryService binaryService;

    public TstpXmlServiceImpl(TstpBinaryService binaryService) {
        this.binaryService = binaryService;
    }

    @Override
    public List<Measurement> parseXmlGetResponseToMeasurements(String responseBody) {
        XmlTsData responseObject = unmarshalXmlTsData(responseBody);
        LOG.debug("unmarshalled get response");

        return parseXmlTsDataToMeasurementList(responseObject);
    }
    @Override
    public XmlQueryResponse parseXmlCatalog(String xmlCatalog) {
        return unmarshalXmlCatalog(xmlCatalog);
    }

    @Override
    public XmlTsResponse parseXmlPutResponse(String xml) {
        return unmarshalXMlTsResponse(xml);
    }

    @Override
    public String parseXmlPutRequest(List<Measurement> measurements) {
        byte[] binaryBlock = binaryService.encode(measurements);
        String binaryEncoded = insertNewlines(Base64.getEncoder().encodeToString(binaryBlock));

        XmlTsDefinition xmlTsDef = new XmlTsDefinition(
                "Z",
                "Nein",
                "K",
                "cm",
                String.valueOf(measurements.size()*12),
                String.valueOf(measurements.size())
        );
        XmlTsData xmlTsData = new XmlTsData("1", xmlTsDef, binaryEncoded);
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
            throw new RuntimeException("There was an error marshalling the XmlTsData");
        }
    }

    private XmlTsResponse unmarshalXMlTsResponse(String xml) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(XmlTsResponse.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            StringReader reader = new StringReader(xml);

            return (XmlTsResponse) unmarshaller.unmarshal(reader);
        } catch (JAXBException e) {
            throw new RuntimeException("There was an error unmarshalling the XmlTsResponse");
        }
    }


    private XmlQueryResponse unmarshalXmlCatalog(String xmlCatalog) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(XmlQueryResponse.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            StringReader reader = new StringReader(xmlCatalog);

            return (XmlQueryResponse) unmarshaller.unmarshal(reader);
        } catch (JAXBException e) {
            throw new RuntimeException("There was an error unmarshalling the XML Catalog");
        }
    }

    private List<Measurement> parseXmlTsDataToMeasurementList(XmlTsData data) {
        String rawMeasurements = data.getData().replace("\n", "");
        byte[] decoded = Base64.getDecoder().decode(rawMeasurements);

        return binaryService.decode(decoded);
    }



    private XmlTsData unmarshalXmlTsData(String xml) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(XmlTsData.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            StringReader reader = new StringReader(xml);

            return (XmlTsData) unmarshaller.unmarshal(reader);
        } catch (JAXBException e) {
            throw new RuntimeException("There was an error unmarshalling the XML");
        }
    }

    private String insertNewlines(String inputString) {
        StringBuilder sb = new StringBuilder(inputString);
        int i = 60;
        while (i < sb.length()) {
            sb.insert(i, "\n");
            i += 60 + 1; // +1 to account for new \n
        }
        return sb.toString();
    }
}
