package com.projectlounge.utils;

import org.junit.Test;

import java.util.Arrays;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.*;

/**
 * Created by main on 28.08.17.
 */
public class HashesAnyTest {

    @Test
    public void combine() throws Exception {
        final HashesAny hashes = newHashesByte3(1,2,3, 7,8,9);
        final Hashes combine = hashes.combine(new byte[]{4,5,6});
        assertTrue(combine.matches(new byte[]{1,2,3}));
        assertTrue(combine.matches(new byte[]{4,5,6}));
        assertTrue(combine.matches(new byte[]{7,8,9}));
    }

    @Test
    public void remove() throws Exception {
        final HashesAny hashes = newHashesByte3(1,2,3, 4,5,6, 7,8,9);
        final Hashes combine = hashes.remove(new byte[]{7,8,9});
        assertTrue(combine.matches(new byte[]{1,2,3}));
        assertTrue(combine.matches(new byte[]{4,5,6}));
        assertFalse(combine.matches(new byte[]{7,8,9}));
    }

    @Test
    public void hashesByte1() throws Exception {
        final HashesAny hashes = newHashesByte(15);
        testHashesByte(hashes, 1, 2, 3, 4, 5);
        testHashesByte(hashes, 6, 7, 8, 9, 10);
        testHashesByte(hashes, 11, 12, 13, 14, 15);
    }

    @Test
    public void hashesByte2() throws Exception {
        final HashesAny hashes = newHashesByte(5);
        testHashesByte(hashes, 1, 2, 3, 4, 5);
    }

    @Test
    public void hashesByte3() throws Exception {
        final HashesAny hashes = newHashesByte(10);
        testHashesByte(hashes, 1, 2, 3, 4, 5);
        testHashesByte(hashes, 6, 7, 8, 9, 10);
    }

    @Test
    public void hashesByte4() throws Exception {
        final HashesAny hashes = newHashesByte(5);
        testHashesByte(hashes, 1, 2, 3, 4, 5);
    }

    @Test
    public void hashesByteNegative1() throws Exception {
        final HashesAny hashes = newHashesByte(25);
        final boolean notMatches = hashes.matches(new byte[]{1,2,3,4,0});
        assertFalse("Inexistent hash has been found!", notMatches);
    }

    @Test
    public void hashesByteNegative2() throws Exception {
        final HashesAny hashes = newHashesByte(25);
        final boolean notMatches = hashes.matches(new byte[]{11,12,13,14,0});
        assertFalse("Inexistent hash has been found!", notMatches);
    }

    @Test
    public void hashesByteNegative3() throws Exception {
        final HashesAny hashes = newHashesByte(25);
        final boolean notMatches = hashes.matches(new byte[]{11,12,13,14,20});
        assertFalse("Inexistent hash has been found!", notMatches);
    }

    @Test
    public void hashesByteNegative4() throws Exception {
        final HashesAny hashes = newHashesByte(25);
        final boolean notMatches = hashes.matches(new byte[]{21,22,23,24,0});
        assertFalse("Inexistent hash has been found!", notMatches);
    }

    @Test
    public void hashesByteTooSmall() throws Exception {
        final HashesAny hashes = newHashesByte(5);
        try {
            hashes.matches(new byte[]{0, 0, 0, 0});
        } catch (IllegalArgumentException e) {
            return;
        }
        fail("Worked on hash length smaller than hash prefix length!");
    }

    private HashesAny newHashesByte(final int size) {
        final byte[] input = new byte[size];
        for (int i = 0; i < size; i++) {
            input[i] = (byte)(i+1);
        }
        return new HashesAny(input, 5);
    }

    private HashesAny newHashesByte3(final int... bytes) {
        final byte[] input = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            input[i] = (byte) bytes[i];
        }
        return new HashesAny(input, 3);
    }

    private void testHashesByte(final Hashes hashes, final int... expected) {
        final byte[] match = getBytes(expected);
        final boolean matches = hashes.matches(match);
        assertTrue(String.format("Hash '%s' not found!", Arrays.toString(match)), matches);
    }

    private byte[] getBytes(final int[] expected) {
        final byte[] match = new byte[expected.length];
        int i = 0;
        for (int each : expected) {
            match[i++] = (byte) each;
        }
        return match;
    }

}