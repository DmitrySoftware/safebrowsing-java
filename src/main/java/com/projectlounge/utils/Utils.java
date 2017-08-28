package com.projectlounge.utils;

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

}
