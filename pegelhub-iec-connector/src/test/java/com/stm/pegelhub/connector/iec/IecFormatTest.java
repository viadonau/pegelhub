package com.stm.pegelhub.connector.iec;

import com.stm.pegelhub.lib.PegelHubCommunicator;
import com.stm.pegelhub.lib.model.Measurement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openmuc.j60870.*;
import org.openmuc.j60870.ie.*;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IecFormatTest {

    @Mock
    private Connection connection;

    @Mock
    private PegelHubCommunicator communicator;

    @Test
    void testASduTypeCorrect() throws Exception {
        ArgumentCaptor<ASdu> asduCaptor = ArgumentCaptor.forClass(ASdu.class);

        processAndSendTestMeasurements(42);

        verify(connection).send(asduCaptor.capture());
        ASdu capturedASdu = asduCaptor.getValue();

        // ASdu should have the type M_ME_TF_1
        assertEquals(ASduType.M_ME_TF_1, capturedASdu.getTypeIdentification());
    }

    @Test
    void testInformationObjectAddressCorrect() throws Exception {
        ArgumentCaptor<ASdu> asduCaptor = ArgumentCaptor.forClass(ASdu.class);

        int expectedIoa = 42;
        processAndSendTestMeasurements(expectedIoa);

        verify(connection).send(asduCaptor.capture());
        ASdu capturedASdu = asduCaptor.getValue();

        InformationObject io = capturedASdu.getInformationObjects()[0];
        assertEquals(expectedIoa, io.getInformationObjectAddress());
    }

    @Test
    void testInformationElementValues() throws Exception {
        ArgumentCaptor<ASdu> asduCaptor = ArgumentCaptor.forClass(ASdu.class);

        Map<String, Double> fields = new HashMap<>();
        fields.put("level", 123.45);
        Map<String, String> infos = new HashMap<>();
        infos.put("IOA", "42");

        Measurement measurement = new Measurement(fields, infos);
        List<Measurement> measurements = List.of(measurement);

        processAndSendTestMeasurements(42, measurements);

        verify(connection).send(asduCaptor.capture());
        ASdu capturedASdu = asduCaptor.getValue();

        InformationObject io = capturedASdu.getInformationObjects()[0];
        InformationElement[][] elements = io.getInformationElements();

        assertEquals(1, elements.length);

        IeShortFloat valueElement = (IeShortFloat) elements[0][0];
        assertEquals(123.45f, valueElement.getValue(), 0.001);

        IeQuality qualityElement = (IeQuality) elements[0][1];
        assertFalse(qualityElement.isBlocked());
        assertFalse(qualityElement.isInvalid());
        assertFalse(qualityElement.isOverflow());
        assertFalse(qualityElement.isSubstituted());
    }

    @Test
    void testNullValueHandling() throws Exception {
        Map<String, Double> fields = new HashMap<>();
        fields.put("level", null);
        fields.put("temperature", 22.5);

        Map<String, String> infos = new HashMap<>();
        infos.put("IOA", "42");

        Measurement measurement = new Measurement(fields, infos);
        List<Measurement> measurements = List.of(measurement);

        processAndSendTestMeasurements(42, measurements);

        ArgumentCaptor<ASdu> asduCaptor = ArgumentCaptor.forClass(ASdu.class);
        verify(connection).send(asduCaptor.capture());

        ASdu capturedASdu = asduCaptor.getValue();
        InformationObject io = capturedASdu.getInformationObjects()[0];
        InformationElement[][] elements = io.getInformationElements();

        assertEquals(1, elements.length);

        IeShortFloat valueElement = (IeShortFloat) elements[0][0];
        assertEquals(22.5f, valueElement.getValue(), 0.001);
    }

    @Test
    void testEmptyMeasurementHandling() throws Exception {
        // Leere Measurement
        Map<String, Double> fields = new HashMap<>();
        Map<String, String> infos = new HashMap<>();
        infos.put("IOA", "42");

        Measurement measurement = new Measurement(fields, infos);
        List<Measurement> measurements = List.of(measurement);

        // Methode aufrufen
        processAndSendTestMeasurements(42, measurements);

        // Es sollte kein ASdu gesendet werden
        verify(connection, never()).send(any(ASdu.class));
    }

    @Test
    void testAllNullValuesHandling() throws Exception {
        // Measurement mit nur null-Werten
        Map<String, Double> fields = new HashMap<>();
        fields.put("level", null);
        fields.put("temperature", null);

        Map<String, String> infos = new HashMap<>();
        infos.put("IOA", "42");

        Measurement measurement = new Measurement(fields, infos);
        List<Measurement> measurements = List.of(measurement);

        // Methode aufrufen
        processAndSendTestMeasurements(42, measurements);

        // Es sollte kein ASdu gesendet werden
        verify(connection, never()).send(any(ASdu.class));
    }

    @Test
    void testIoaFromInfos() throws Exception {
        ArgumentCaptor<ASdu> asduCaptor = ArgumentCaptor.forClass(ASdu.class);

        // Measurement mit expliziter IOA
        Map<String, Double> fields = new HashMap<>();
        fields.put("level", 123.45);

        Map<String, String> infos = new HashMap<>();
        infos.put("IOA", "99");  // IOA explizit in infos angegeben

        Measurement measurement = new Measurement(fields, infos);
        List<Measurement> measurements = List.of(measurement);

        // Methode aufrufen mit einer anderen IOA als der in infos
        processAndSendTestMeasurements(42, measurements);

        // ASdu überprüfen - sollte die IOA aus infos verwenden
        verify(connection).send(asduCaptor.capture());
        ASdu capturedASdu = asduCaptor.getValue();

        // Die IOA sollte dem Wert entsprechen, den wir der Methode übergeben haben
        InformationObject io = capturedASdu.getInformationObjects()[0];
        assertEquals(42, io.getInformationObjectAddress());
    }

    @Test
    void testCauseOfTransmission() throws Exception {
        ArgumentCaptor<ASdu> asduCaptor = ArgumentCaptor.forClass(ASdu.class);

        processAndSendTestMeasurements(42);

        verify(connection).send(asduCaptor.capture());
        ASdu capturedASdu = asduCaptor.getValue();

        // Überprüfen der Übertragungsursache
        assertEquals(CauseOfTransmission.SPONTANEOUS, capturedASdu.getCauseOfTransmission());
    }

    // Hilfsmethoden für die Tests

    private List<Measurement> createTestMeasurements() {
        Map<String, Double> fields = new HashMap<>();
        fields.put("level", 123.45);

        Map<String, String> infos = new HashMap<>();
        infos.put("IOA", "42");

        Measurement measurement = new Measurement(fields, infos);
        return List.of(measurement);
    }

    private void processAndSendTestMeasurements(int ioa) throws IOException, InterruptedException {
        processAndSendTestMeasurements(ioa, createTestMeasurements());
    }

    private void processAndSendTestMeasurements(int ioa, List<Measurement> measurements)
            throws IOException, InterruptedException {

        List<InformationElement[]> elements = new ArrayList<>();

        for (Measurement measurement : measurements) {
            Map<String, Double> fields = measurement.getFields();

            if (fields.isEmpty()) {
                continue;
            }

            for (Map.Entry<String, Double> field : fields.entrySet()) {
                Double value = field.getValue();

                if (value == null) {
                    continue;
                }

                InformationElement[] element = {
                        new IeShortFloat(value.floatValue()),
                        new IeQuality(false, false, false, false, false)
                };

                elements.add(element);
            }
        }

        if (elements.isEmpty()) {
            return;
        }

        InformationElement[][] ieArray = elements.toArray(new InformationElement[0][]);
        InformationObject informationObject = new InformationObject(ioa, ieArray);

        ASdu asdu = new ASdu(
                ASduType.M_ME_TF_1, true,
                CauseOfTransmission.SPONTANEOUS, false,
                false, 0, 1, informationObject);

        connection.send(asdu);
    }
}