package com.projectlounge.model;

import lombok.Data;
import lombok.ToString;

/**
 * Created by main on 24.08.17.
 */
@Data
@ToString
public class ThreatListState {
    private String state;

    public String getState() {
        if (null==state) return "";
        return state;
    }
}
