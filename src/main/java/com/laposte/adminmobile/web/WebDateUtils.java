package com.laposte.adminmobile.web;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Shared helper for the operations/transactions/consultations screens: every filter form in this
 * module uses a native {@code <input type="date">} field (value format {@code yyyy-MM-dd}), in
 * place of the legacy PrimeFaces {@code <p:calendar pattern="dd/MM/yyyy">}. Controllers keep the
 * raw request strings around (to echo them back into the form on redisplay) and use this to parse
 * them into {@link Date} for the service layer.
 */
final class WebDateUtils {

    private WebDateUtils() {
    }

    static boolean hasBoth(String date1, String date2) {
        return date1 != null && !date1.isBlank() && date2 != null && !date2.isBlank();
    }

    static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** Returns null on a missing/unparseable value; callers surface that as a form error. */
    static Date parse(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
            fmt.setLenient(false);
            return fmt.parse(value.trim());
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Like {@link #parse} but returns the last millisecond of that day: the end bound of a filter
     * period must include the whole day, otherwise "du 01/01 au 31/01" stops at 31/01 00:00:00 and
     * silently drops every row timestamped on the 31st.
     */
    static Date parseEnd(String value) {
        Date date = parse(value);
        if (date == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }
}
