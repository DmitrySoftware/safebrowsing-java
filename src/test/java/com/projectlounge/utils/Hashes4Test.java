package com.projectlounge.utils;

import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.*;

/**
 * Created by main on 28.08.17.
 */
public class Hashes4Test {

    @Test
    public void hashesInt() throws Exception {
        final byte[] input = new byte[]{ 1,2,3,4, 5,6,7,8, 9,10,11,12 };
        final Hashes4 hashes = new Hashes4(input);
        final boolean matches = hashes.matches(new byte[]{5, 6, 7, 8});
        assertTrue("Hash not found!", matches);
        final boolean notMatches = hashes.matches(new byte[]{0, 0, 0, 0});
        assertFalse("Inexistent hash has been found!", notMatches);
    }

    @Test
    public void combine1() throws Exception {
        final Hashes4 hashesInt = new Hashes4(new byte[]{1,1,1,1, 2,2,2,2, 3,3,3,1});
        final Hashes combine = hashesInt.combine(new byte[]{3,3,3,3});
        assertTrue(combine.matches(new byte[]{1,1,1,1}));
        assertTrue(combine.matches(new byte[]{2,2,2,2}));
        assertTrue(combine.matches(new byte[]{3,3,3,1}));
        assertTrue(combine.matches(new byte[]{3,3,3,3}));
    }

    @Test
    public void combine2() throws Exception {
        final Hashes4 hashesInt = new Hashes4(new byte[]{1,1,1,1});
        final Hashes combine = hashesInt.combine(new byte[]{0,3,3,3});
        assertTrue(combine.matches(new byte[]{1,1,1,1}));
        assertTrue(combine.matches(new byte[]{0,3,3,3}));
    }

    @Test
    public void delete() throws Exception {
        final Hashes4 hashesInt = new Hashes4(new byte[]{1,1,1,1, 2,2,2,2, 3,3,3,3});
        final Hashes combine = hashesInt.remove(new byte[]{2,2,2,2});
        assertTrue(combine.matches(new byte[]{1,1,1,1}));
        assertFalse(combine.matches(new byte[]{2,2,2,2}));
        assertTrue(combine.matches(new byte[]{3,3,3,3}));
    }


}