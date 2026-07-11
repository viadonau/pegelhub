package at.pegelhub.connector.livetest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

final class TstpBinaryCodec {
    private TstpBinaryCodec() {
    }

    static List<MeasurementRecord> decode(UUID timeSeriesId, byte[] bytes) {
        List<MeasurementRecord> measurements = new ArrayList<>();
        if (bytes.length % 12 != 0) {
            return measurements;
        }
        for (int offset = 0; offset < bytes.length; offset += 12) {
            byte[] dateBytes = Arrays.copyOfRange(bytes, offset, offset + 8);
            int year = 0;
            int month = 0;
            int day = 0;
            int hours = 0;
            int minutes = 0;
            int seconds = 0;
            for (int i = 0; i < dateBytes.length; i++) {
                byte dateByte = dateBytes[i];
                if (i == 1) {
                    for (int bit = 3; bit >= 0; bit--) {
                        year = (year << 1) | getBit(dateByte, bit);
                    }
                }
                if (i == 2) {
                    for (int bit = 7; bit >= 0; bit--) {
                        year = (year << 1) | getBit(dateByte, bit);
                    }
                }
                if (i == 3) {
                    for (int bit = 3; bit >= 0; bit--) {
                        month = (month << 1) | getBit(dateByte, bit);
                    }
                }
                if (i == 4) {
                    for (int bit = 4; bit >= 0; bit--) {
                        day = (day << 1) | getBit(dateByte, bit);
                    }
                }
                if (i == 5) {
                    hours = (hours << 8) | (dateByte & 0xFF);
                }
                if (i == 6) {
                    minutes = (minutes << 8) | (dateByte & 0xFF);
                }
                if (i == 7) {
                    seconds = (seconds << 8) | (dateByte & 0xFF);
                }
            }
            int bits = ByteBuffer.wrap(Arrays.copyOfRange(bytes, offset + 8, offset + 12))
                    .order(java.nio.ByteOrder.BIG_ENDIAN)
                    .getInt();
            double value = BigDecimal.valueOf(Float.intBitsToFloat(bits)).setScale(2, RoundingMode.HALF_UP).doubleValue();
            Instant observedAt = LocalDateTime.of(year, month, day, hours, minutes, seconds).toInstant(ZoneOffset.UTC);
            measurements.add(new MeasurementRecord(timeSeriesId, observedAt, value));
        }
        return measurements;
    }

    static byte[] encode(List<MeasurementRecord> measurements) {
        byte[] binaryBlock = new byte[measurements.size() * 12];
        for (int i = 0; i < measurements.size(); i++) {
            MeasurementRecord measurement = measurements.get(i);
            LocalDateTime timestamp = LocalDateTime.ofInstant(measurement.observedAt(), ZoneOffset.UTC);
            byte[] dateBytes = new byte[8];
            dateBytes[0] = 0;
            dateBytes[1] = (byte) ((timestamp.getYear() >> 8) & 0x0F);
            dateBytes[2] = (byte) (timestamp.getYear() & 0xFF);
            dateBytes[3] = (byte) (timestamp.getMonthValue() & 0x0F);
            dateBytes[4] = (byte) (timestamp.getDayOfMonth() & 0x1F);
            dateBytes[5] = (byte) (timestamp.getHour() & 0xFF);
            dateBytes[6] = (byte) (timestamp.getMinute() & 0xFF);
            dateBytes[7] = (byte) (timestamp.getSecond() & 0xFF);
            byte[] floatBytes = ByteBuffer.allocate(4)
                    .order(java.nio.ByteOrder.BIG_ENDIAN)
                    .putInt(Float.floatToIntBits((float) measurement.value()))
                    .array();
            int copyTo = i * 12;
            System.arraycopy(dateBytes, 0, binaryBlock, copyTo, 8);
            System.arraycopy(floatBytes, 0, binaryBlock, copyTo + 8, 4);
        }
        return binaryBlock;
    }

    private static int getBit(byte in, int position) {
        return (in >> position) & 1;
    }
}
