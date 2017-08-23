package com.projectlounge.json;

import lombok.Data;
import lombok.ToString;

/**
 * Created by main on 23.08.17.
 */
@Data
@ToString
public class ThreatList {
    private final String threatType;
    private final String platformType;
    private final String threatEntryType;

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final ThreatList that = (ThreatList) o;

        if (threatType != null ? !threatType.equals(that.threatType) : that.threatType != null) return false;
        if (platformType != null ? !platformType.equals(that.platformType) : that.platformType != null) return false;
        return threatEntryType != null ? threatEntryType.equals(that.threatEntryType) : that.threatEntryType == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (threatType != null ? threatType.hashCode() : 0);
        result = 31 * result + (platformType != null ? platformType.hashCode() : 0);
        result = 31 * result + (threatEntryType != null ? threatEntryType.hashCode() : 0);
        return result;
    }
}
