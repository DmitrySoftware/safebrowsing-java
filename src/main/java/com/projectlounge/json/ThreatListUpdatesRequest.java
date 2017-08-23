package com.projectlounge.json;

import lombok.Data;
import lombok.ToString;

/**
 * Created by main on 24.08.17.
 */
@Data
@ToString
public class ThreatListUpdatesRequest {
    private ClientInfo clientInfo;
    private ListUpdateRequest[] listUpdateRequests;
}
