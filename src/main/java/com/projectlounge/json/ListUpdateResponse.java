package com.projectlounge.json;

import com.projectlounge.json.enums.PlatformType;
import com.projectlounge.json.enums.ResponseType;
import com.projectlounge.json.enums.ThreatEntryType;
import com.projectlounge.json.enums.ThreatType;
import lombok.Data;

/**
 * Created by main on 24.08.17.
 */
@Data
public class ListUpdateResponse {

    private ThreatType threatType;
    private ThreatEntryType threatEntryType;
    private PlatformType platformType;
    private ResponseType responseType;
    private ThreatEntrySet[] additions;
    private ThreatEntrySet[] removals;
    private String newClientState;
    private Checksum checksum;

}
