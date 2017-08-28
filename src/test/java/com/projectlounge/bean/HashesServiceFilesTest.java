package com.projectlounge.bean;

import com.projectlounge.BaseTest;
import org.junit.Test;

import javax.inject.Inject;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Base64;

import static org.junit.Assert.*;

/**
 * Created by main on 28.08.17.
 */
public class HashesServiceFilesTest extends BaseTest {

    @Inject private HashesServiceFiles test;

    @Test
    public void updateCache() throws Exception {
//        test.updateCache();
    }

}