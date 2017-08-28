package com.projectlounge.json;

import com.projectlounge.json.enums.CompressionType;
import lombok.Data;

/**
 * Created by main on 24.08.17.
 */
@Data
public class ThreatEntrySet {

    private CompressionType compressionType;
    private RawHashes rawHashes;
    private RawIndices rawIndices;
    private RiceDeltaEncoding riceHashes;
    private RiceDeltaEncoding riceIndices;

}
