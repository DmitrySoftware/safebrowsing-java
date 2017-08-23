package com.projectlounge.utils;

import java.util.Arrays;

/**
 * Created by main on 23.08.17.
 */
public class HashesInt implements Hashes {

    private final int[] hash;

    public HashesInt(final byte[] input) {
        final int length = input.length;
        if (length % 4 != 0) throw new IllegalArgumentException("Incorrect prefix length or input length!");
        hash = new int[length / 4];
        int j = 0;
        for (int i = 0; i < length; i = i + 4) {
            int hashPrefix = buildKey(input, i);
            hash[j++] = hashPrefix;
        }
    }

    @Override
    public boolean matches(final byte[] input) {
        if (input.length < 4) throw new IllegalArgumentException("Hash is too small!");
        final int key = buildKey(input);
        return Arrays.binarySearch(hash, key) >= 0;
    }

    private int buildKey(final byte[] hash) {
        return buildKey(hash, 0);
    }

    private static int buildKey(final byte[] hash, final int i) {
        int result = hash[i];
        result = result << 4;
        result |= hash[i+1];
        result = result << 4;
        result |= hash[i+2];
        result = result << 4;
        result |= hash[i+3];
        return result;
    }
}
