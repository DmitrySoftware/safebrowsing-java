package com.projectlounge.bean;

import com.projectlounge.json.Constraints;
import com.projectlounge.json.ListUpdateRequest;
import com.projectlounge.json.ListUpdateResponse;
import com.projectlounge.json.ThreatEntrySet;
import com.projectlounge.json.ThreatList;
import com.projectlounge.json.ThreatListUpdatesResponse;
import com.projectlounge.json.enums.CompressionType;
import com.projectlounge.json.enums.PlatformType;
import com.projectlounge.json.enums.ThreatEntryType;
import com.projectlounge.json.enums.ThreatType;
import com.projectlounge.utils.Hashes;
import com.projectlounge.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by main on 23.08.17.
 */
@Component
@Slf4j
public class HashesServiceInMemory implements HashesService {

    private final ConcurrentMap<ThreatList, String> threatListStateMap;
    private final ConcurrentMap<ThreatList, Hashes> hashesMap;
    private final HashUtils hashUtils;
    private final UpdateApi updateApi;
    private final AtomicLong nextUpdate;
    private final Create create;

    @Inject
    public HashesServiceInMemory(final HashUtils hashUtils, final UpdateApi updateApi, final Create create) {
        this.create = create;
        this.threatListStateMap  = new ConcurrentHashMap<>();
        this.hashesMap = new ConcurrentHashMap<>();
        this.nextUpdate = new AtomicLong(0);
        this.hashUtils = hashUtils;
        this.updateApi = updateApi;
    }

    @Override
    public void updateCache() {
        if (minimumWaitTimeNotReached()) {
            log.info("Update cache skipped: minimum wait time not reached.");
            return;
        }
        try {
            log.info("Executing update cache...");
            final long time = executeUpdateCache();
            nextUpdate.set(time);
            log.info("[DONE] Executing update cache, next update time: [{}]", nextUpdate.get());
        } catch (Throwable t) {
            log.error("[FAILURE] Executing update cache!", t);
        }
    }

    private long executeUpdateCache() {
        final Set<ThreatList> threatLists = updateApi.loadThreatLists();
        final ListUpdateRequest[] requests = createListUpdateRequests(threatLists);
        final ThreatListUpdatesResponse response = updateApi.loadThreatListUpdates(requests);
        for (ListUpdateResponse update : response.getListUpdateResponses()) {
            final ThreatList threatList = createThreatList(update);
            log.info("Creating hash for [{}]", threatList);
            final Hashes hashes = hashesMap.get(threatList);
            final Hashes newHashes = newHashes(update, hashes);
            hashesMap.put(threatList, newHashes);
            threatListStateMap.put(threatList, update.getNewClientState());
        }
        return getNextUpdateTime(response.getMinimumWaitDuration());
    }

    private Hashes newHashes(final ListUpdateResponse update, final Hashes hashes) {
        return (null==hashes) ? createNew(update) : updateHashes(update, hashes);
    }

    private Hashes createNew(final ListUpdateResponse update) {
        final LinkedList<ThreatEntrySet> threats = new LinkedList<>(Arrays.asList(update.getAdditions()));
        final ThreatEntrySet threat = threats.pollFirst();
        final byte[] bytes = getHashes(threat);
        Hashes hashes = create.hashes(bytes, threat.getRawHashes().getPrefixSize());
        for (ThreatEntrySet eachThreat : threats) {
            final byte[] newBytes = getHashes(eachThreat);
            hashes = hashes.combine(newBytes);
        }
        return hashes;
    }

    private Hashes updateHashes(final ListUpdateResponse update, Hashes hashes) {
        final ThreatEntrySet[] removals = update.getRemovals();
        if (null != removals) {
            for (ThreatEntrySet removal : removals) {
                hashes = remove(hashes, removal);
            }
        }
        final ThreatEntrySet[] additions = update.getAdditions();
        if (null != additions) {
            for (ThreatEntrySet addition : additions) {
                hashes = add(hashes, addition);
            }
        }
        return hashes;
    }

    long getNextUpdateTime(final String minimumWaitDuration) {
        if (StringUtils.isEmpty(minimumWaitDuration)) return 0;
        final String s = minimumWaitDuration.substring(0, minimumWaitDuration.length() - 1);
        final double seconds = Double.parseDouble(s);
        final long newMinimumWaitTime = (long) Math.ceil(seconds);
        return System.currentTimeMillis() + (newMinimumWaitTime * 1000);
    }


    private Hashes add(final Hashes hashes, final ThreatEntrySet addition) {
        final byte[] bytes = getHashes(addition);
        return hashes.combine(bytes);
    }

    private byte[] getHashes(final ThreatEntrySet threat) {
        final String rawHashes = threat.getRawHashes().getRawHashes();
        return Base64.getDecoder().decode(rawHashes);
    }

    private Hashes remove(final Hashes hashes, final ThreatEntrySet removal) {
        return hashes.remove(getHashes(removal));
    }

    private ThreatList createThreatList(final ListUpdateResponse update) {
        final ThreatList threatList = new ThreatList();
        threatList.setThreatType(update.getThreatType().name());
        threatList.setThreatEntryType(update.getThreatEntryType().name());
        threatList.setPlatformType(update.getPlatformType().name());
        return threatList;
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
            request.setState(threatListStateMap.getOrDefault(threatList, ""));
            result[i++] = request;
        }
        return result;
    }

    private Constraints getConstraints() {
        final Constraints constraints = new Constraints();
        constraints.setMaxUpdateEntries(0);
        constraints.setMaxDatabaseEntries(0);
        constraints.setRegion("");
        constraints.setSupportedCompressions(new CompressionType[]{ CompressionType.RAW }); //todo raw
        return constraints;
    }

    private boolean minimumWaitTimeNotReached() {
        return System.currentTimeMillis() < nextUpdate.get();
    }

    @Override
    public Optional<Boolean> matches(final String url) {
        final List<byte[]> urlHashes = hashUtils.makeHashesBinary(url);
        for (Map.Entry<ThreatList,Hashes> entry : hashesMap.entrySet()) {
            final Hashes hashes = entry.getValue();
            for (byte[] urlHash : urlHashes) {
                if (hashes.matches(urlHash)) return Optional.of(Boolean.TRUE);
            }
        }
        return Optional.of(Boolean.FALSE);
    }

    @Override
    public Optional<Boolean> matches(final String url, final Collection<ThreatList> threatlists) {
        final List<byte[]> urlHashes = hashUtils.makeHashesBinary(url);
        for (ThreatList threatList : threatlists) {
            final Hashes hashes = hashesMap.get(threatList);
            if (null == hashes) return Optional.empty();
            for (byte[] urlHash : urlHashes) {
                if (hashes.matches(urlHash)) return Optional.of(Boolean.TRUE);
            }
        }
        return Optional.of(Boolean.FALSE);
    }


}
