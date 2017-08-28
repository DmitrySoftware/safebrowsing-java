package com.projectlounge.bean;

import com.projectlounge.json.ThreatList;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;

/**
 * Created by main on 23.08.17.
 */
@Component
public interface HashesService {

    void updateCache();
    Optional<Boolean> matches(String url);
    Optional<Boolean> matches(String url, Collection<ThreatList> threatlists);

}
