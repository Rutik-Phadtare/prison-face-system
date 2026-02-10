package com.prison.util;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;

public class TimeUtil {

    private TimeUtil() {
        // Prevent object creation
    }

    /**
     * Calculates remaining sentence time from release date
     */
    public static String calculateRemainingTime(LocalDate releaseDate) {

        if (releaseDate == null) return "Release date not set";

        LocalDate now = LocalDate.now();

        if (!releaseDate.isAfter(now)) {
            return "Sentence Completed";
        }

        Period p = Period.between(now, releaseDate);

        return String.format(
                "%d years, %d months, %d days",
                p.getYears(), p.getMonths(), p.getDays()
        );
    }

}
