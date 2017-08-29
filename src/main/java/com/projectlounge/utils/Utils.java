package com.projectlounge.utils;

import org.springframework.util.StringUtils;

/**
 * Created by main on 28.08.17.
 */
public class Utils {

    public static <T extends Enum<T>> T findByName(final String name, final Class<T> clazz) {
        try {
            return Enum.valueOf(clazz, name.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    public static long getNextUpdateTime(final String minimumWaitDuration) {
        if (StringUtils.isEmpty(minimumWaitDuration)) return 0;
        final String s = minimumWaitDuration.substring(0, minimumWaitDuration.length() - 1);
        final double seconds = Double.parseDouble(s);
        final long newMinimumWaitTime = (long) Math.ceil(seconds);
        return System.currentTimeMillis() + (newMinimumWaitTime * 1000);
    }

}
