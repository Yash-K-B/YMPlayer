package com.yash.ymplayer.util;

import android.content.Context;
import android.util.TypedValue;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DecimalFormat;

public class ConverterUtil {
    public static float getPx(Context context, int dip) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dip,
                context.getResources().getDisplayMetrics()
        );
    }

    public static String toStringStackTrace(Throwable throwable) {
        if (throwable == null)
            return "";
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        return stringWriter.toString();
    }

    public static String toStringException(Exception e) {
        return toStringStackTrace(e);
    }

    private static short byteToShortLE(byte b1, byte b2) {
        return (short) (b1 & 0xFF | ((b2 & 0xFF) << 8));
    }

    public static int bytesToChannels(byte[] bytes, int count, short[] left, short[] right) {

        int index = 0;
        for (int i = 0; i < count; i += 2) {
            short val = byteToShortLE(bytes[i], bytes[i + 1]);
            if (i % 4 == 0) {
                left[index] = val;
            } else {
                right[index] = val;
                index++;
            }
        }

        return index;
    }

    public static int bytesToChannels(byte[] bytes, int count, short[] left) {

        int index = 0;
        for (int i = 0; i < count; i += 2) {
            short val = byteToShortLE(bytes[i], bytes[i + 1]);
            left[index] = val;
            index++;
        }

        return index;
    }

    public static int bytesToShorts(byte[] bytes, int count, short[] left) {

        int index = 0;
        for (int i = 0; i < count; i += 2) {
            short val = byteToShortLE(bytes[i], bytes[i + 1]);
            left[index] = val;
            index++;
        }

        return index;
    }

    public static long convertStringLengthToLong(String length) {
        String duration = length.replace("PT", "").replace("H", ":").replace("M", ":").replace("S", "");
        String[] splits = duration.split(":", 3);
        long song_duration = 0L;
        int pos = splits.length - 1;
        for (int r = pos; r >= 0; r--) {
            if (splits[r].isEmpty()) splits[r] = "0";
            song_duration += Long.parseLong(splits[r]) * Math.pow(60, pos - r);
        }
        return song_duration * 1000;
    }

    public static String getFormattedFileSize(long fileSize) {
        DecimalFormat df = new DecimalFormat("0.00");
        if (fileSize >= 1073741824) {
            return df.format(fileSize / 1073741824.0) + " GB";
        } else if (fileSize >= 1048576) {
            return df.format(fileSize / 1048576.0) + " MB";
        } else if (fileSize >= 1024) {
            return df.format(fileSize / 1024.0) + " KB";
        } else {
            return fileSize + " B";
        }
    }

    public static String toString(Object obj) {
        if (obj == null)
            return null;
        return String.valueOf(obj);
    }
}
