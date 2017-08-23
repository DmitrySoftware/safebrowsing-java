package com.projectlounge.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Created by main on 23.08.17.
 */
public class HashesByte implements Hashes {

    private static final Logger log = LoggerFactory.getLogger(HashesByte.class.getName());

    private final byte[] hashes;
    private final int hashLen;

    /**
     * Assuming input is sorted.
     * @param input hashes
     */
    public HashesByte(final byte[] input, final int hashPrefixLength) {
        final int length = input.length;
        if (length % hashPrefixLength != 0) throw new IllegalArgumentException("Incorrect hash prefix length or hash array length!");
        hashLen = hashPrefixLength;
        hashes = Arrays.copyOf(input, length);
    }


    @Override
    public boolean matches(final byte[] input) {
        if (hashLen > input.length) throw new IllegalArgumentException("Hash is too small!");
        return binarySearch(input) >= 0;
    }

    private int binarySearch(final byte[] input) {
        int l = 0;
        int r = hashes.length;
        final byte key = input[0]; // first byte of the hash
        main:
        while (l <= r) {
            final int x = (l + r) >>> 1; // middle
            final int m = x - (x % hashLen); // correct according to the start of the hash
            int value = hashes[m];
            if (value > key) {
                r = m - 1;
            } else if (value < key) {
                l = m + hashLen;
            } else {
                // rest of the hash
                for (int i = 1; i < hashLen; i++) {
                    value = hashes[m + i];
                    final byte subKey = input[i];
                    if (value > subKey) {
                        r = m - 1;
                        break main;
                    } else if (value < subKey) {
                        l = m + hashLen;
                        break main;
                    }
                }
                return m;
            }
        }
        return -1;
    }
}
