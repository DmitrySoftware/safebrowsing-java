package com.projectlounge.bean;

import com.projectlounge.json.ListUpdateResponse;
import com.projectlounge.json.RawHashes;
import com.projectlounge.json.ThreatEntrySet;
import com.projectlounge.json.ThreatList;
import com.projectlounge.json.enums.CompressionType;
import com.projectlounge.utils.HashesServiceBase;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Collection;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by main on 27.08.17.
 */
@Service
@Slf4j
public class HashesServiceFiles extends HashesServiceBase {

    private final Create create;
    private final HashStorage hashStorage;

    @Value("${threatLists.path}")
    private String threatListsDir;


    @Inject
    public HashesServiceFiles(final UpdateApi updateApi, final Create create) throws IOException {
        super(updateApi);
        this.create = create;
        this.hashStorage = new HashStorage(getThreatListsDir());
    }

    @Override
    protected void fullUpdate(final ListUpdateResponse update) throws IOException {
        final String listName = getThreatListName(update);
        for (ThreatEntrySet set : update.getAdditions()) {
            if (CompressionType.RAW != set.getCompressionType()) continue; //todo compression
            final RawHashes rawHashes = set.getRawHashes();
            final int prefixSize = rawHashes.getPrefixSize();
            final byte[] bytes = Base64.getDecoder().decode(rawHashes.getRawHashes());
            getHashStorage().newHashes(listName, prefixSize, bytes);
            break;
        }
    }

    @Override
    protected void partialUpdate(final ListUpdateResponse update) {
        //todo implement
    }

    @Override
    protected void onUpdateSuccess() {
        getHashStorage().switchFileSet();
    }

    @Override
    public Optional<Boolean> matches(final String url) {
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> matches(final String url, final Collection<ThreatList> threatLists) {
        return Optional.empty();
    }

    @Override
    protected String getState(final ThreatList threatList) {
        return getHashStorage().getState(threatList);
    }

    private HashStorage getHashStorage() {
        return hashStorage;
    }

    @Data
    @Slf4j
    private static class HashStorage {

        private static final String LIST_NAME = "listName";
        private static final String PREFIX_SIZE = "prefixSize";
        private static final String LENGTH = "length";
        private static final String STATE = "state";

        private final AtomicBoolean fileSetSwitch;
        private final String threatListDir;
        private final Properties properties1;
        private final Properties properties2;

        HashStorage(final String threatListDir) throws IOException {
            this.threatListDir = threatListDir;
            this.fileSetSwitch = new AtomicBoolean(true);
            this.properties1 = loadProperties("1");
            this.properties2 = loadProperties("2");
        }

        private Properties loadProperties(final String suffix) throws IOException {
            final Properties properties = new Properties();
            final String filePath = threatListDir + File.separator + "__threatListMeta__" + suffix;
            final Path path = Paths.get(filePath);
            createFile(filePath);
            properties.load(Files.newBufferedReader(path));
            return properties;
        }

        void switchFileSet() {
            fileSetSwitch.set(!fileSetSwitch.get());
        }

        void newHashes(final String listName, final int prefixSize, final byte[] bytes) throws IOException {
            final String threatListPath = getThreatListPath(listName);
            final File file = createFile(threatListPath);
            try (final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
                 final FileChannel channel = randomAccessFile.getChannel()) {
                final MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_WRITE, 0, bytes.length);
                map.put(bytes);
                map.force();
            }
            updateMetaInfo(listName, prefixSize, bytes.length);
        }

        void addHashes(final String listName, final int prefixSize, final byte[] bytes) throws IOException {
            final String threatListPath = getThreatListPath(listName);
            final File file = createFile(threatListPath);
            try (final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
                 final FileChannel channel = randomAccessFile.getChannel()) {
                final int listLength = getLength(listName);
                final MappedByteBuffer map = channel.map(
                        FileChannel.MapMode.READ_WRITE, listLength, bytes.length + listLength
                );
                map.put(bytes);
                map.force();
            }
            updateMetaInfo(listName, prefixSize, bytes.length);
        }

        String getState(final ThreatList threatList) {
            final String threatListName = getListName(threatList);
            final String stateKey = getStateKey(threatListName);
            return getProperties().getProperty(stateKey);
        }

        private int getPrefixSize(final String listName) {
            final String key = getPrefixSizeKey(listName);
            return getIntProperty(key);
        }

        private int getLength(final String listName) {
            final String key = getLengthKey(listName);
            return getIntProperty(key);
        }

        private int getIntProperty(final String key) {
            final String property = getProperties().getProperty(key);
            return Integer.parseInt(property);
        }

        private void updateMetaInfo(final String listName, final int prefixSize, final int length) {
            final Properties properties = getProperties();
            properties.setProperty(getPrefixSizeKey(listName), String.valueOf(prefixSize));
            properties.setProperty(getLengthKey(listName), String.valueOf(length));
        }

        private String getLengthKey(final String listName) {
            return getKey(listName, LENGTH);
        }

        private String getPrefixSizeKey(final String listName) {
            return getKey(listName, PREFIX_SIZE);
        }

        private String getStateKey(final String listName) {
            return getKey(listName, STATE);
        }

        private String getKey(final String listName, final String param) {
            return listName + "___" + param;
        }

        private Properties getProperties() {
            return fileSetSwitch.get() ? properties1 : properties2;
        }

        private String getThreatListPath(final String listName) {
            final String suffix = fileSetSwitch.get() ? "_1" : "_2";
            return threatListDir + File.separator + listName + suffix;
        }

        private File createFile(final String path) throws IOException {
            final File file = Paths.get(path).toFile();
            final File parentFile = file.getParentFile();
            if (parentFile.mkdirs()) log.info("Directory created [{}]", parentFile);
            if (file.createNewFile()) log.info("File created [{}]", file);
            return file;
        }

        private String getListName(final ThreatList threatList) {
            return String.join("_",
                    threatList.getPlatformType(),
                    threatList.getThreatType(),
                    threatList.getThreatEntryType()
            );
        }

    }

    private String getThreatListName(final ListUpdateResponse update) {
        return String.join("_",
                update.getPlatformType().name(),
                update.getThreatType().name(),
                update.getThreatEntryType().name());
    }

    private String getThreatListsDir() {
        return threatListsDir;
    }

}
