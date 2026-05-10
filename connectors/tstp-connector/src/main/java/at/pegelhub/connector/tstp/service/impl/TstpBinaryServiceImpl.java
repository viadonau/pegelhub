package at.pegelhub.connector.tstp.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import at.pegelhub.connector.tstp.service.TstpBinaryService;
import at.pegelhub.lib.model.Measurement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TstpBinaryServiceImpl implements TstpBinaryService {
    private static final Logger LOG = LoggerFactory.getLogger(TstpBinaryServiceImpl.class);

    @Override
    public List<Measurement> decode(byte[] toDecode) {
        List<Measurement> measurementList = new ArrayList<>();

        if(toDecode.length % 12 != 0){
            LOG.info("Binary array has an invalid length - measurements are not parsed");
            return measurementList;
        }

        // can probably be done with one loop, but I don't have enough time to change it (something with modulo should work)
        for (int j = 0; j < toDecode.length; j = j + 12) {
            byte[] dateBytes = Arrays.copyOfRange(toDecode, j, j + 8);

            int year = 0;
            int month = 0;
            int day = 0;

            int hours = 0;
            int minutes = 0;
            int seconds = 0;
            for (int k = 0; k < dateBytes.length; k++) {
                byte dateByte = dateBytes[k];

                //comments are referring the table 1 from the tstp documentation
                //Byte 6
                if (k == 1) {
                    for (int i = 3; i >= 0; i--) {
                        int bit = getBit(dateByte, i);
                        year = (year << 1) | bit;
                    }
                }
                //Byte 5
                if (k == 2) {
                    for (int i = 7; i >= 0; i--) {
                        int bit = getBit(dateByte, i);
                        year = (year << 1) | bit;
                    }
                }
                //Byte 4
                if (k == 3) {
                    for (int i = 3; i >= 0; i--) {
                        int bit = getBit(dateByte, i);
                        month = (month << 1) | bit;
                    }
                }
                //Byte 3
                if (k == 4) {
                    for (int i = 4; i >= 0; i--) {
                        int bit = getBit(dateByte, i);
                        day = (day << 1) | bit;
                    }
                }

                //Byte 2
                if (k == 5) {
                    hours = (hours << 8) | (dateByte & 0xFF);
                }
                //Byte 1
                if (k == 6) {
                    minutes = (minutes << 8) | (dateByte & 0xFF);
                }
                //Byte 0
                if (k == 7) {
                    seconds = (seconds << 8) | (dateByte & 0xFF);
                }
            }
            int bits = ByteBuffer.wrap(Arrays.copyOfRange(toDecode, j + 8, j + 12)).order(java.nio.ByteOrder.BIG_ENDIAN).getInt();
            double ieeeFloat = Float.intBitsToFloat(bits);
            double roundedFloat = BigDecimal.valueOf(ieeeFloat).setScale(2, RoundingMode.HALF_UP).doubleValue();
            LocalDateTime dateTime = LocalDateTime.of(year, month, day, hours, minutes, seconds);

            HashMap<String, Double> valueMap = new HashMap<>();
            valueMap.put("value", roundedFloat);

            // TODO infos necessary??
            measurementList.add(new Measurement(dateTime, valueMap, new HashMap<>()));
        }
        return measurementList;
    }

    @Override
    public byte[] encode(List<Measurement> toEncode) {
        int byteArraySize = toEncode.size() * 12;
        byte[] binaryBlock = new byte[byteArraySize];

        for (int i = 0; i < toEncode.size(); i++) {
            Measurement currentMeasurement = toEncode.get(i);
            LocalDateTime timestamp = currentMeasurement.getTimestamp();
            float measurementValue = (float) currentMeasurement.getFields().get("value").doubleValue();

            byte[] dateBytes = new byte[8];
            // Byte 7
            dateBytes[0] = (byte) 0;
            // Byte 6
            dateBytes[1] = (byte) ((timestamp.getYear() >> 8) & 0x0F);
            // Byte 5
            dateBytes[2] = (byte) (timestamp.getYear() & 0xFF);
            // Byte 4
            dateBytes[3] = (byte) (timestamp.getMonthValue() & 0x0F);
            // Byte 3
            dateBytes[4] = (byte) (timestamp.getDayOfMonth() & 0x1F);
            // Byte 2
            dateBytes[5] = (byte) (timestamp.getHour() & 0xFF);
            // Byte 1
            dateBytes[6] = (byte) (timestamp.getMinute() & 0xFF);
            // Byte 0
            dateBytes[7] = (byte) (timestamp.getSecond() & 0xFF);

            // float to 4 bytes (32 bit)
            int bits = Float.floatToIntBits(measurementValue);
            byte[] floatBytes = ByteBuffer.allocate(4).order(java.nio.ByteOrder.BIG_ENDIAN).putInt(bits).array();

			// 12 bytes ( 8 date bytes + 4 value bytes)
			int copyToPosition = i * 12;
			System.arraycopy(dateBytes, 0, binaryBlock, copyToPosition, 8);
			System.arraycopy(floatBytes, 0, binaryBlock, copyToPosition + 8, 4);
		}

        return binaryBlock;
    }

    private int getBit(byte in, int position) {
        return (in >> position) & 1;
    }
}
