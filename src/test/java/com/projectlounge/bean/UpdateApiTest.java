package com.projectlounge.bean;

import com.projectlounge.BaseTest;
import com.projectlounge.json.Constraints;
import com.projectlounge.json.ListUpdateRequest;
import com.projectlounge.json.ThreatList;
import com.projectlounge.json.enums.CompressionType;
import com.projectlounge.json.enums.PlatformType;
import com.projectlounge.json.enums.ThreatEntryType;
import com.projectlounge.json.enums.ThreatType;
import org.junit.Test;

import javax.inject.Inject;
import java.util.Set;

import static org.junit.Assert.assertNotNull;

/**
 * Created by main on 24.08.17.
 */
public class UpdateApiTest extends BaseTest {

    @Inject
    private UpdateApi test;

    @Test
    public void loadThreatListUpdates() throws Exception {
        final ListUpdateRequest request1 = getListUpdateRequest();
        final ListUpdateRequest[] requests = new ListUpdateRequest[]{request1};
        test.loadThreatListUpdates(requests);
    }

    @Test
    public void loadThreatLists() throws Exception {
        final Set<ThreatList> threatLists = test.loadThreatLists();
        assertNotNull("Null threat lists", threatLists);
    }

    private ListUpdateRequest getListUpdateRequest() {
        final ListUpdateRequest request1 = new ListUpdateRequest();
        request1.setThreatType(ThreatType.MALWARE);
        request1.setThreatEntryType(ThreatEntryType.URL);
        request1.setPlatformType(PlatformType.WINDOWS);
        request1.setState("");
        final Constraints constraints = new Constraints();
        constraints.setRegion("");
        constraints.setMaxDatabaseEntries(0);
        constraints.setMaxUpdateEntries(0);
        constraints.setSupportedCompressions(new CompressionType[]{CompressionType.RAW});
        request1.setConstraints(constraints);
        return request1;
    }

}