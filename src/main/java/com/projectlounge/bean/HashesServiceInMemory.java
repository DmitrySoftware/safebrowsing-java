package com.projectlounge.bean;

import com.projectlounge.json.ListUpdateResponse;
import com.projectlounge.json.ThreatEntrySet;
import com.projectlounge.json.ThreatList;
import com.projectlounge.json.enums.CompressionType;
import com.projectlounge.utils.Hashes;
import com.projectlounge.utils.HashesServiceBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by main on 23.08.17.
 */
@Service
@Slf4j
public class HashesServiceInMemory extends HashesServiceBase {

    private final ConcurrentMap<ThreatList, String> threatListStateMap;
    private final ConcurrentMap<ThreatList, Hashes> hashesMap;
    private final HashUtils hashUtils;
    private final Create create;

    @Inject
    public HashesServiceInMemory(final HashUtils hashUtils, final UpdateApi updateApi, final Create create) {
        super(updateApi);
        this.create = create;
        this.threatListStateMap  = new ConcurrentHashMap<>();
        this.hashesMap = new ConcurrentHashMap<>();
        this.hashUtils = hashUtils;
    }

    @Override
    protected void fullUpdate(final ListUpdateResponse update) {
        final ThreatList threatList = createThreatList(update);
        log.info("Running full hashes update for [{}]", threatList);
        final Hashes newHashes = createNew(update);
        hashesMap.put(threatList, newHashes);
        threatListStateMap.put(threatList, update.getNewClientState());
    }

    @Override
    protected void partialUpdate(final ListUpdateResponse update) {
        final ThreatList threatList = createThreatList(update);
        log.info("Running partial hashes update for [{}]", threatList);
        final Hashes hashes = hashesMap.get(threatList);
        final Hashes newHashes = combineHashes(update, hashes);
        hashesMap.put(threatList, newHashes);
        threatListStateMap.put(threatList, update.getNewClientState());
    }

    @Override
    protected String getState(final ThreatList threatList) {
        return threatListStateMap.getOrDefault(threatList, "");
    }

    private Hashes combineHashes(final ListUpdateResponse update, final Hashes hashes) {
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
        hashes = transform(hashes, update.getRemovals(), this::remove);
        hashes = transform(hashes, update.getAdditions(), this::add);
        return hashes;
    }

    private Hashes transform(Hashes hashes, final ThreatEntrySet[] threatEntrySets, final Transform transform) {
        if (null == threatEntrySets) return hashes;
        for (ThreatEntrySet set : threatEntrySets) {
            if (CompressionType.RAW != set.getCompressionType()) continue; //todo compression
            hashes = transform.apply(hashes, set);
        }
        return hashes;
    }

    private interface Transform {
        Hashes apply(Hashes hashes, ThreatEntrySet set);
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
