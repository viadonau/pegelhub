//package at.pegelhub.lib;
//
//import at.pegelhub.lib.internal.ApplicationProperties;
//import at.pegelhub.lib.model.Supplier;
//
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.stream.Collectors;
//
//public class InfluxID {
//    private long ID = 0;
//    private PegelHubCommunicator communicator;
//    private ApplicationProperties props;
//    private Boolean IDfetched = false;
//    private LocalDateTime lastCheck;
//
//    public InfluxID(PegelHubCommunicator communicator, ApplicationProperties props)
//    {
//        this.communicator = communicator;
//        this.props = props;
//        ID = calculateID();
//    }
//
//    public long calculateID()
//    {
//        Collection<Supplier> sups = communicator.getSuppliers();
//
//        Set<Supplier> fromProperties;
//
//        fromProperties = sups.stream().filter(supplier -> props.getSupplier().stationNumber().equals(supplier.getStationNumber())).collect(Collectors.toSet());
//
//        Optional<Supplier> optionalWork;
//        Supplier work = null;
//
//        if(fromProperties.size() == 1)
//        {
//            optionalWork = fromProperties.stream().findFirst();
//            work = optionalWork.get();
//        }
//
//        LocalDateTime checkTime = communicator.getSystemTime().toLocalDateTime();
//
//        long seconds = 0;
//        seconds = checkTime.getHour()*60*60;
//        seconds = seconds + checkTime.getMinute()*60;
//        seconds = seconds + checkTime.getSecond();
//
//        if(!IDfetched) {
//            HashSet<Long> IDs = null;
//            if (work != null) {
//                IDs = communicator.getMeasurementsIDsOfStation(work.getStationNumber(), seconds + "s");
//            }
//
//            List<Long> listIDs = null;
//            if (IDs != null) {
//                listIDs = new ArrayList<>(IDs.stream().toList());
//            }
//
//            if (listIDs != null && !listIDs.isEmpty()) {
//                Long max = Collections.max(listIDs);
//                if (max != 0L) {
//                    IDfetched = true;
//                    ID = max + 1;
//                }
//            }
//        }
//        if(lastCheck != null) {
//            if (lastCheck.getYear() < checkTime.getYear())
//            {
//                ID = 0;
//            } else if (lastCheck.getMonth().getValue() < checkTime.getMonth().getValue()) {
//                ID = 0;
//            } else if (lastCheck.getMonth() == checkTime.getMonth()) {
//                if (lastCheck.getDayOfMonth() < checkTime.getDayOfMonth()) {
//                    ID = 0;
//                }
//            }
//        }
//        lastCheck = checkTime;
//        return ID;
//    }
//
//    public void setID(long ID)
//    {
//        this.ID = ID;
//    }
//
//    public void addID()
//    {
//        ID = ID + 1;
//    }
//
//    public long getIDValue()
//    {
//        return this.ID;
//    }
//
//}
