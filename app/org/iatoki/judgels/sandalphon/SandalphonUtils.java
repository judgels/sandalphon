package org.iatoki.judgels.sandalphon;

import org.apache.commons.io.FileUtils;

import java.text.SimpleDateFormat;

public final class SandalphonUtils {
    private SandalphonUtils() {
        // prevent instantiation
    }

    public static String timestampToFormattedDate(long timestamp) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd - HH:mm");
        return formatter.format(timestamp);
    }

    public static String byteCountToFormattedSize(long bytes) {
        return FileUtils.byteCountToDisplaySize(bytes);
    }
}
