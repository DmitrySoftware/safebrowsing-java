package com.projectlounge.bean;

import com.projectlounge.BaseTest;
import org.junit.Test;

import javax.inject.Inject;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by main on 28.08.17.
 */
public class HashesServiceInMemoryRealTest extends BaseTest {

    @Inject
    private HashesServiceInMemory test;


    @Test
    public void updateCache() throws Exception {
        final boolean matches = test.matches(TEST_URL).get();
        assertFalse("Hit on empty cache!", matches);
        test.updateCache();
        assertTrue("Test URL not found!", test.matches(TEST_URL).get());
    }


}