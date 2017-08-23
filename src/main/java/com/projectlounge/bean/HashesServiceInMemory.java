package com.projectlounge.bean;

import com.projectlounge.json.ThreatList;
import com.projectlounge.json.ThreatLists;
import com.projectlounge.model.ThreatListState;
import com.projectlounge.utils.Hashes;
import com.projectlounge.utils.Utils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by main on 23.08.17.
 */
public class HashesServiceInMemory implements HashesService {

    private final Map<ThreatList, ThreatListState> threatListStateMap = new HashMap<>(); //todo concurrent?
    private final Map<ThreatList, Hashes> hashesMap = new HashMap<>(); //todo concurrent?

    @Override
    public void updateCache() {
        final Collection<ThreatList> threatLists = loadThreatLists();

    }

    private Collection<ThreatList> loadThreatLists() {
        final String url = Utils.getThreatListsUrl();
        final RestTemplate rest = new RestTemplate();
        final ResponseEntity<ThreatLists> entity = rest.getForEntity(url, ThreatLists.class);
        final ThreatLists threatLists = entity.getBody();
        return Arrays.asList(threatLists.getThreatLists());
    }

}
