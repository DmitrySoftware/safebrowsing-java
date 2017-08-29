package com.projectlounge.utils;

import com.projectlounge.json.enums.PlatformType;
import org.junit.Test;

import java.util.Calendar;

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

    @Test
    public void getNextUpdateTime() throws Exception {
        final Calendar c1 = Calendar.getInstance();
        final Calendar c2 = Calendar.getInstance();
        c2.setTime(c1.getTime());
        final long nextUpdateTime = Utils.getNextUpdateTime("15.1s");
        c2.setTimeInMillis(nextUpdateTime);
        c1.add(Calendar.SECOND, 16);
        c1.set(Calendar.MILLISECOND, 0);
        c2.set(Calendar.MILLISECOND, 0);
        assertEquals(c1, c2);
    }


}