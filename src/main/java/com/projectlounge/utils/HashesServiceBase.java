package com.projectlounge.utils;

import com.projectlounge.bean.HashesService;
import com.projectlounge.bean.UpdateApi;
import com.projectlounge.json.Constraints;
import com.projectlounge.json.ListUpdateRequest;
import com.projectlounge.json.ListUpdateResponse;
import com.projectlounge.json.ThreatList;
import com.projectlounge.json.ThreatListUpdatesResponse;
import com.projectlounge.json.enums.CompressionType;
import com.projectlounge.json.enums.PlatformType;
import com.projectlounge.json.enums.ResponseType;
import com.projectlounge.json.enums.ThreatEntryType;
import com.projectlounge.json.enums.ThreatType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by main on 29.08.17.
 */
@Slf4j
public abstract class HashesServiceBase implements HashesService {

    private final UpdateApi updateApi;
    private final AtomicLong nextUpdate;

    public HashesServiceBase(final UpdateApi updateApi) {
        this.updateApi = updateApi;
        this.nextUpdate = new AtomicLong(0);
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

    private boolean minimumWaitTimeNotReached() {
        return System.currentTimeMillis() < nextUpdate.get();
    }

    private long executeUpdateCache() throws Exception {
        final Set<ThreatList> threatLists = updateApi.loadThreatLists();
        final ListUpdateRequest[] requests = createListUpdateRequests(threatLists);
        final ThreatListUpdatesResponse response = updateApi.loadThreatListUpdates(requests);
        for (ListUpdateResponse update : response.getListUpdateResponses()) {
            final ResponseType responseType = getResponseType(update);
            switch (responseType) {
                case PARTIAL_UPDATE:
                    partialUpdate(update);
                    break;
                case FULL_UPDATE:
                    fullUpdate(update);
                    break;
                default:
                    throw new RuntimeException("Unknown response type!");
            }
        }
        onUpdateSuccess();
        return Utils.getNextUpdateTime(response.getMinimumWaitDuration());
    }

    protected void onUpdateSuccess() {}

    private ListUpdateRequest[] createListUpdateRequests(final Collection<ThreatList> threatLists) {
        final ListUpdateRequest[] result = new ListUpdateRequest[threatLists.size()];
        int i = 0;
        for (ThreatList threatList : threatLists) {
            final ListUpdateRequest request = new ListUpdateRequest();
            request.setPlatformType(Utils.findByName(threatList.getPlatformType(), PlatformType.class));
            request.setThreatEntryType(Utils.findByName(threatList.getThreatEntryType(), ThreatEntryType.class));
            request.setThreatType(Utils.findByName(threatList.getThreatType(), ThreatType.class));
            request.setConstraints(getConstraints());
            request.setState(getState(threatList));
            result[i++] = request;
        }
        return result;
    }

    protected abstract String getState(final ThreatList threatList);

    private Constraints getConstraints() {
        final Constraints constraints = new Constraints();
        constraints.setMaxUpdateEntries(0);
        constraints.setMaxDatabaseEntries(0);
        constraints.setRegion("");
        constraints.setSupportedCompressions(new CompressionType[]{ CompressionType.RAW }); //todo raw
        return constraints;
    }

    private ResponseType getResponseType(final ListUpdateResponse update) {
        final ResponseType responseType = update.getResponseType();
        if (null==responseType) {
            log.error("Null response type, counting as unspecified.");
            return ResponseType.RESPONSE_TYPE_UNSPECIFIED;
        }
        return responseType;
    }

    protected abstract void partialUpdate(final ListUpdateResponse update) throws Exception;

    protected abstract void fullUpdate(final ListUpdateResponse update) throws Exception;

}
