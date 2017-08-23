package com.projectlounge.json;

import com.projectlounge.json.enums.PlatformType;
import com.projectlounge.json.enums.ThreatEntryType;
import com.projectlounge.json.enums.ThreatType;
import lombok.Data;
import lombok.ToString;

/**
 * Created by main on 24.08.17.
 */
@Data
@ToString
public class ListUpdateRequest {

    private PlatformType platformType;
    private ThreatEntryType threatEntryType;
    private ThreatType threatType;

}
