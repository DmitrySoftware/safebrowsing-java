package com.projectlounge.json;

import lombok.Data;

/**
 * Created by main on 24.08.17.
 */
@Data
public class ThreatListUpdatesResponse {

    private ListUpdateResponse[] listUpdateResponses;
    private String minimumWaitDuration;

}
