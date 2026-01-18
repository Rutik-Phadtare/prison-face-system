package com.prison.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class TimeUtil {

    private TimeUtil() {
        // Prevent object creation
    }

    /**
     * Calculates remaining sentence time from release date
     */
    public static String calculateRemainingTime(LocalDate releaseDate) {

        if (releaseDate == null) {
            return "Release date not set";
        }

        long daysRemaining =
                ChronoUnit.DAYS.between(LocalDate.now(), releaseDate);

        if (daysRemaining <= 0) {
            return "Sentence Completed";
        }

        long years = daysRemaining / 365;
        long months = (daysRemaining % 365) / 30;
        long days = (daysRemaining % 365) % 30;

        StringBuilder result = new StringBuilder();

        if (years > 0) result.append(years).append(" year(s) ");
        if (months > 0) result.append(months).append(" month(s) ");
        if (days > 0) result.append(days).append(" day(s)");

        return result.toString().trim() + " remaining";
    }
}
