package com.gezbox.monitor;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by zombie on 14/12/4.
 */
public class MonitorUtils {
    public static String getCurrentTimeStr() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeStr = dateFormat.format(new Date());
        return timeStr;
    }

    public static String getDateStr() {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String timeStr = dateFormat.format(new Date());
        return timeStr;
    }
}
