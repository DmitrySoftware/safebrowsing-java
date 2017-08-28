package com.projectlounge.bean;

import com.projectlounge.json.ClientInfo;
import com.projectlounge.json.ListUpdateRequest;
import com.projectlounge.json.ListUpdateResponse;
import com.projectlounge.json.ThreatList;
import com.projectlounge.json.ThreatListUpdatesRequest;
import com.projectlounge.json.ThreatListUpdatesResponse;
import com.projectlounge.json.ThreatLists;
import com.projectlounge.utils.Constants;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by main on 24.08.17.
 */
@Service
@Getter
@Slf4j
public class UpdateApi {

    private final ClientInfo client;
    private final String threatListsUrl;
    private final String threatListFetchUrl;

    @Inject
    public UpdateApi(final AppProperties prop) {
        final String apiKey = prop.get(Constants.API_KEY);
        threatListsUrl = "https://safebrowsing.googleapis.com/v4/threatLists?key=" + apiKey;
        threatListFetchUrl = "https://safebrowsing.googleapis.com/v4/threatListUpdates:fetch?key=" + apiKey;
        client = new ClientInfo(prop.get(Constants.CLIENT_ID), prop.get(Constants.CLIENT_VERSION));
    }

    public Set<ThreatList> loadThreatLists() {
        log.info("Loading threat lists...");
        final RestTemplate rest = new RestTemplate();
        final String url = getThreatListsUrl();
        final ResponseEntity<ThreatLists> entity = rest.getForEntity(url, ThreatLists.class);
        final ThreatList[] threatLists = entity.getBody().getThreatLists();
        final Set<ThreatList> result = new HashSet<>(threatLists.length);
        result.addAll(Arrays.asList(threatLists));
        log.info("[SUCCESS] Loading threat lists total: [{}] [{}].", result.size(), result);
        return result;
    }

    public ThreatListUpdatesResponse loadThreatListUpdates(final ListUpdateRequest[] listUpdateRequests) {
        log.info("Loading threat list updates...");
        final RestTemplate rest = new RestTemplate();
        final ThreatListUpdatesRequest request = new ThreatListUpdatesRequest();
        request.setClient(getClient());
        request.setListUpdateRequests(listUpdateRequests);
        final String url = getThreatListFetchUrl();
        final ResponseEntity<ThreatListUpdatesResponse> entity = rest.postForEntity(url, request, ThreatListUpdatesResponse.class);
        final ThreatListUpdatesResponse response = entity.getBody();
        final ListUpdateResponse[] updatesList = response.getListUpdateResponses();
        log.info("[SUCCESS] Loading threat list updates: [{}].", (null== updatesList) ? 0 : updatesList.length);
        return response;
    }

}
