package org.example;

import com.stm.pegelhub.lib.PegelHubCommunicator;
import com.stm.pegelhub.lib.internal.ApplicationProperties;
import com.stm.pegelhub.lib.internal.ApplicationPropertiesImpl;
import com.stm.pegelhub.lib.model.Measurement;
import com.stm.pegelhub.lib.model.Supplier;

import java.time.OffsetDateTime;
import java.util.*;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

public class ConnectorTask extends TimerTask {

    private PegelHubCommunicator communicator;
    private ConnectorOptions conOpts;
    private ApplicationProperties properties;
//    private InfluxID influxID;
    String revPiInput;
    static
    {
        System.loadLibrary("RevPiReader");
    }

    public ConnectorTask(PegelHubCommunicator communicator, ConnectorOptions conOpts)
    {
        this.communicator = communicator;
        this.conOpts = conOpts;
        this.properties = new ApplicationPropertiesImpl(conOpts.getPropertiesFile());
//        this.influxID = new InfluxID(communicator, properties);
        revPiInput = conOpts.getInputOnRevpi();
    }
    private RevPiReader revPiReader = new RevPiReader();
    @Override
    public void run() {

        Collection<Supplier> sups = communicator.getSuppliers();

        Set<Supplier> fromProperties;

        fromProperties = sups.stream().filter(supplier -> properties.getSupplier().stationNumber().equals(supplier.getStationNumber())).collect(Collectors.toSet());

        Optional<Supplier> optionalWork;
        Supplier work = null;

        if(fromProperties.size() == 1)
        {
            optionalWork = fromProperties.stream().findFirst();
            work = optionalWork.get();
        }

        UUID supUUID = UUID.fromString(work.getId());
//        influxID.calculateID();

        int revPiInputInt = Integer.parseInt(revPiInput);

        int valueFromOffset = revPiReader.readFromOffset(revPiInputInt);

        System.out.println("Value from revpi: " + valueFromOffset);

        HashMap<String, Double> fields = new HashMap<>();
        Double writeValue = (double) valueFromOffset;
        fields.put("Value", writeValue);

        OffsetDateTime now = OffsetDateTime.now();
        HashMap<String, String> infos = new HashMap<>();
//        infos.put("ID", String.valueOf(influxID.getIDValue()));
        infos.put("Connector Name", "4...20mA Connector");
        infos.put("TimestampWithOffset", now.toString());
        infos.put("Quality", "Placeholder");
        infos.put("Error", "Placeholder");
        infos.put("ChannelUse", work.getChannelUse());
        infos.put("Type", work.getChannelUse());

        Measurement measurement = new Measurement(fields, infos);
	    measurement.setTimestamp(LocalDateTime.now());
        List<Measurement> measurementList = new ArrayList<>();

        System.out.println("Value in measurement: " + measurement.getFields().get("Value"));

        measurementList.add(measurement);
        communicator.sendMeasurements(measurementList);
//	    influxID.addID();
    }
}
