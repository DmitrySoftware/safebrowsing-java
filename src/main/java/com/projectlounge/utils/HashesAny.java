package com.projectlounge.utils;

import lombok.NonNull;

import java.util.Arrays;
import java.util.TreeSet;

/**
 * Created by main on 23.08.17.
 */
public class HashesAny implements Hashes {

    private final TreeSet<Hash> hashes;
    private final int prefixLength;

    public HashesAny(@NonNull final byte[] input, final int hashPrefixLength) {
        if (input.length % hashPrefixLength != 0) throw new IllegalArgumentException("Failed to create Hashes: Incorrect hash prefix length or hash array length!");
        prefixLength = hashPrefixLength;
        hashes = getHashes(input, hashPrefixLength);
    }

    private HashesAny(final TreeSet<Hash> input, final int hashPrefixLength) {
        hashes = input;
        prefixLength = hashPrefixLength;
    }

    private static TreeSet<Hash> getHashes(final @NonNull byte[] input, final int hashPrefixLength) {
        final TreeSet<Hash> result = new TreeSet<>();
        for (int i = 0; i < input.length; i = i + hashPrefixLength) {
            final Hash hash = new Hash(input, i, hashPrefixLength);
            result.add(hash);
        }
        return result;
    }

    @Override
    public boolean matches(@NonNull final byte[] hash) {
        if (prefixLength > hash.length) throw new IllegalArgumentException("Failed to find a match: Hash is too small!");
        return hashes.contains(new Hash(hash, 0, prefixLength));
    }

    @Override
    public Hashes combine(final byte[] hash) {
        final TreeSet<Hash> newHashes = new TreeSet<>(hashes);
        newHashes.addAll(getHashes(hash, prefixLength));
        return new HashesAny(newHashes, prefixLength);
    }

    @Override
    public Hashes remove(final byte[] hash) {
        final TreeSet<Hash> newHashes = new TreeSet<>(hashes);
        final TreeSet<Hash> removal = getHashes(hash, prefixLength);
        newHashes.removeAll(removal);
        return new HashesAny(newHashes, prefixLength);
    }

    private static class Hash implements Comparable<Hash> {

        private final byte[] data;

        Hash(final byte[] input, final int pos, final int size) {
            data = new byte[size];
            System.arraycopy(input, pos, data, 0, size);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final Hash hash = (Hash) o;

            return Arrays.equals(data, hash.data);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(data);
        }

        @Override
        public int compareTo(final Hash o) {
            for (int i = 0; i < data.length; i++) {
                final int compare = Byte.compare(data[i], o.data[i]);
                if (compare != 0) return compare;
            }
            return 0;
        }
    }
}
