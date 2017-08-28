package com.projectlounge.json;

import com.projectlounge.json.enums.CompressionType;
import lombok.Data;

/**
 * Created by main on 24.08.17.
 */
@Data
public class Constraints {
    private Integer maxUpdateEntries;
    private Integer maxDatabaseEntries;
    private String region;
    private CompressionType[] supportedCompressions;

}
