package com.projectlounge.utils;

import com.projectlounge.json.enums.PlatformType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by main on 08.08.17.
 */
public class UtilsTest {

    @Test
    public void findByName() throws Exception {
        final PlatformType actual1 = Utils.findByName("winDows", PlatformType.class);
        assertEquals(PlatformType.WINDOWS, actual1);
        final PlatformType actual2 = Utils.findByName("test", PlatformType.class);
        assertEquals(null, actual2);
    }


}