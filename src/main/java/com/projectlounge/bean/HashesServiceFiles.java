package com.projectlounge.bean;

import com.projectlounge.json.Constraints;
import com.projectlounge.json.ListUpdateRequest;
import com.projectlounge.json.ListUpdateResponse;
import com.projectlounge.json.RawHashes;
import com.projectlounge.json.ThreatEntrySet;
import com.projectlounge.json.ThreatList;
import com.projectlounge.json.ThreatListUpdatesResponse;
import com.projectlounge.json.enums.CompressionType;
import com.projectlounge.json.enums.PlatformType;
import com.projectlounge.json.enums.ThreatEntryType;
import com.projectlounge.json.enums.ThreatType;
import com.projectlounge.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Created by main on 27.08.17.
 */
@Component
@Slf4j
public class HashesServiceFiles implements HashesService {

    private final UpdateApi updateApi;
    private final Create create;

    @Value("${threatLists.path}")
    private String dir;
    private Calendar nextUpdate;

    @Inject
    public HashesServiceFiles(final UpdateApi updateApi, final Create create) {
        this.updateApi = updateApi;
        this.create = create;
    }

    @Override
    public void updateCache() {
        if (Calendar.getInstance().before(nextUpdate)) return;
        try {
            nextUpdate = executeUpdateCache();
        } catch (Throwable t) {
            log.error("Error updating cache!", t);
        }
    }

    private Calendar executeUpdateCache() throws IOException {
        final Set<ThreatList> threatLists = updateApi.loadThreatLists();
        final ListUpdateRequest[] requests = createListUpdateRequests(threatLists);
        final ThreatListUpdatesResponse response = updateApi.loadThreatListUpdates(requests);
        for (ListUpdateResponse update : response.getListUpdateResponses()) {
            final String listName = String.join("_",
                    update.getPlatformType().name(),
                    update.getThreatType().name(),
                    update.getThreatEntryType().name());
            for (ThreatEntrySet set : update.getAdditions()) {
                final RawHashes rawHashes = set.getRawHashes();
                final Integer prefixSize = rawHashes.getPrefixSize();
                final byte[] bytes = Base64.getDecoder().decode(rawHashes.getRawHashes());
                final Path path = Paths.get(dir + '/' + listName);
                final File f = path.toFile();
                f.getParentFile().mkdirs();
                f.createNewFile();
                final RandomAccessFile file = new RandomAccessFile(f, "rw");
                final FileChannel channel = file.getChannel();
                final MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_WRITE, f.length(), f.length() + bytes.length);
                map.put(bytes);
                map.force();
            }


        }

        return null;
    }

    @Override
    public Optional<Boolean> matches(final String url) {
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> matches(final String url, final Collection<ThreatList> threatLists) {
        return Optional.empty();
    }

    private ListUpdateRequest[] createListUpdateRequests(final Collection<ThreatList> threatLists) {
        final ListUpdateRequest[] result = new ListUpdateRequest[threatLists.size()];
        int i = 0;
        for (ThreatList threatList : threatLists) {
            final ListUpdateRequest request = new ListUpdateRequest();
            request.setPlatformType(Utils.findByName(threatList.getPlatformType(), PlatformType.class));
            request.setThreatEntryType(Utils.findByName(threatList.getThreatEntryType(), ThreatEntryType.class));
            request.setThreatType(Utils.findByName(threatList.getThreatType(), ThreatType.class));
            request.setConstraints(getConstraints());
            request.setState(readState(threatList));
            result[i++] = request;
        }
        return result;
    }

    private String readState(final ThreatList threatList) {
        return null; //todo implement
    }

    private Constraints getConstraints() {
        final Constraints constraints = new Constraints();
        constraints.setSupportedCompressions(new CompressionType[]{ CompressionType.RAW }); //todo raw
        return constraints;
    }
}
