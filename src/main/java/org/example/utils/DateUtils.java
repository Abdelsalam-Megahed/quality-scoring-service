package org.example.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    public static DateTimeFormatter getDateFormat() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd");
    }

    public static LocalDate mapToLocalDate(String date) {
        return LocalDate.parse(date, getDateFormat());
    }

    public static boolean isPeriodLongerThanOneMonth(LocalDate startDate, LocalDate endDate) {
        return startDate.isBefore(endDate.minusMonths(1));
    }
}

