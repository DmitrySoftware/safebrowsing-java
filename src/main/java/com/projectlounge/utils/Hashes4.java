package com.projectlounge.utils;

import lombok.NonNull;

import java.util.Arrays;

/**
 * Created by main on 23.08.17.
 */
public class Hashes4 implements Hashes {

    private static final int PREFIX_LENGTH = 4;
    private final int[] hash;

    public Hashes4(@NonNull final byte[] input) {
        final int length = input.length;
        checkForPrefixLength(length);
        hash = new int[length / PREFIX_LENGTH];
        fill(input, hash);
        Arrays.sort(hash); //todo treat as unsigned int instead ?
    }

    private static void fill(final byte[] src, final int[] dest) {
        int j = 0;
        for (int i = 0; i < src.length; i = i + PREFIX_LENGTH) {
            int hashPrefix = compressToInt(src, i);
            dest[j++] = hashPrefix;
        }
    }

    private Hashes4(final int[] input) {
        hash = input;
        Arrays.sort(hash); //todo treat as unsigned int instead ?
    }

    private void checkForPrefixLength(final int length) {
        if (length % PREFIX_LENGTH != 0) throw new IllegalArgumentException("Incorrect prefix length or input length!");
    }

    @Override
    public boolean matches(@NonNull final byte[] input) {
        if (input.length < PREFIX_LENGTH) throw new IllegalArgumentException("Hash is too small!");
        final int key = compressToInt(input);
        return binarySearch(hash, key) >= 0;
    }

    @Override
    public Hashes combine(@NonNull final byte[] input) {
        checkForPrefixLength(input.length);
        final int[] sorted = new int[input.length / PREFIX_LENGTH];
        fill(input, sorted);
        Arrays.sort(sorted);
        final int[] newHash = new int[hash.length + sorted.length];
        int srcPos = 0;
        int destPos = 0;
        for (final int hashPrefix : sorted) {
            int idx = binarySearch(hash, hashPrefix);
            if (idx < 0) idx = Math.abs(idx) - 1;
            final int length = idx - srcPos;
            System.arraycopy(hash, srcPos, newHash, destPos, length);
            srcPos = srcPos + length;
            destPos = destPos + length;
            newHash[destPos++] = hashPrefix;
        }
        System.arraycopy(hash, srcPos, newHash, destPos, hash.length - srcPos);
        return new Hashes4(newHash);
    }

    @Override
    public Hashes remove(@NonNull final byte[] input) {
        checkForPrefixLength(input.length);
        final int[] sorted = new int[input.length / PREFIX_LENGTH];
        fill(input, sorted);
        Arrays.sort(sorted);
        final int[] newHash = new int[hash.length - sorted.length];
        int srcPos = 0;
        int destPos = 0;
        for (final int hashPrefix : sorted) {
            int idx = binarySearch(hash, hashPrefix);
            if (idx < 0) continue;
            final int length = idx - srcPos;
            System.arraycopy(hash, srcPos, newHash, destPos, length);
            srcPos = srcPos + length + 1;
            destPos = destPos + length;
        }
        System.arraycopy(hash, srcPos, newHash, destPos, hash.length - srcPos);
        return new Hashes4(newHash);
    }

    protected int binarySearch(final int[] hash, final int hashPrefix) {
        return Arrays.binarySearch(hash, hashPrefix);
    }

    @Deprecated
    protected int binarySearch2(final int[] hash, final int key) {
        int l = 0;
        int r = hash.length - 1;
        while (l <= r) {
            final int m = (l + r) >>> 1; // unsigned?
            final int value = hash[m];
            final int compare = Integer.compareUnsigned(value, key);
            if (compare > 0) {
                r = m - 1;
            } else if (compare < 0) {
                l = m + PREFIX_LENGTH;
            } else {
                return m;
            }
        }
        return -(l + 1);
    }

    protected static int compressToInt(final byte[] hash) {
        return compressToInt(hash, 0);
    }

    private static int compressToInt(final byte[] hash, final int i) {
        int result = hash[i];
        result = result << 8;
        result |= hash[i+1];
        result = result << 8;
        result |= hash[i+2];
        result = result << 8;
        result |= hash[i+3];
        return result;
    }

    //todo remove
    public String toString() {
        return "Hashes4{" +
                "hash=" + Arrays.toString(hash) +
                '}';
    }
}
