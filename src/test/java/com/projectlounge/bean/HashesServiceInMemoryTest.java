package com.projectlounge.bean;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.projectlounge.BaseTest;
import com.projectlounge.json.ClientInfo;
import com.projectlounge.json.ListUpdateRequest;
import com.projectlounge.json.ListUpdateResponse;
import com.projectlounge.json.RawHashes;
import com.projectlounge.json.ThreatEntrySet;
import com.projectlounge.json.ThreatList;
import com.projectlounge.json.ThreatListUpdatesRequest;
import com.projectlounge.json.ThreatListUpdatesResponse;
import com.projectlounge.json.enums.PlatformType;
import com.projectlounge.json.enums.ResponseType;
import com.projectlounge.json.enums.ThreatEntryType;
import com.projectlounge.json.enums.ThreatType;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * Created by main on 28.08.17.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class HashesServiceInMemoryTest extends BaseTest {

    private static final String UNWANTED_SOFTWARE_WINDOWS_URL_TXT = "UNWANTED_SOFTWARE_WINDOWS_URL.txt";

    @Inject
    private HashesServiceInMemory test;

    @MockBean
    private UpdateApi updateApi;

    @Before
    public void setup() throws IOException {
        when(updateApi.loadThreatLists()).thenReturn(getThreatLists());
        when(updateApi.loadThreatListUpdates(any())).thenReturn(getThreatListUpdatesResponse(THREAT_LIST_TXT));
    }

//    @Test
    public void saveThreatList() throws Exception {
        final String url = "https://safebrowsing.googleapis.com/v4/threatListUpdates:fetch?key=" + properties.get("apiKey");
        final RestTemplate rest = new RestTemplate();
        final ThreatListUpdatesRequest request = new ThreatListUpdatesRequest();
        final ClientInfo client = new ClientInfo("ProjectLounge", "1.0");
        request.setClient(client);
        final ListUpdateRequest updateRequest = new ListUpdateRequest();
        updateRequest.setThreatType(ThreatType.UNWANTED_SOFTWARE);
        updateRequest.setThreatEntryType(ThreatEntryType.URL);
        updateRequest.setPlatformType(PlatformType.WINDOWS);
        updateRequest.setState("");
        final ListUpdateRequest[] listUpdateRequests = new ListUpdateRequest[]{updateRequest};
        request.setListUpdateRequests(listUpdateRequests);
        final ResponseEntity<String> entity = rest.postForEntity(url, request, String.class);
        final String body = entity.getBody();
        Files.write(Paths.get(PATH + UNWANTED_SOFTWARE_WINDOWS_URL_TXT), body.getBytes("UTF-8"));
    }

    private ThreatListUpdatesResponse getThreatListUpdatesResponse(final String threatListTxt) throws IOException {
        final ThreatListUpdatesResponse response = new ThreatListUpdatesResponse();
        response.setMinimumWaitDuration("5s");
        final ListUpdateResponse update = new ListUpdateResponse();
        final ThreatEntrySet addition = new ThreatEntrySet();
        final String json = new String(Files.readAllBytes(Paths.get(PATH + threatListTxt)), "UTF-8");
        final JsonElement element = new JsonParser().parse(json);
        final JsonElement listUpdateResponses = element.getAsJsonObject().get("listUpdateResponses");
        final JsonElement additionsEl = listUpdateResponses.getAsJsonArray().get(0).getAsJsonObject().get("additions");
        final JsonElement hashes = additionsEl.getAsJsonArray().get(0).getAsJsonObject().get("rawHashes").getAsJsonObject().get("rawHashes");
        final RawHashes rawHashes = new RawHashes();
        rawHashes.setRawHashes(hashes.getAsString());
        rawHashes.setPrefixSize(4);
        addition.setRawHashes(rawHashes);
        final ThreatEntrySet[] additions = new ThreatEntrySet[]{addition};
        update.setAdditions(additions);
        update.setThreatEntryType(ThreatEntryType.URL);
        update.setThreatType(ThreatType.UNWANTED_SOFTWARE);
        update.setPlatformType(PlatformType.WINDOWS);
        final String clientState = listUpdateResponses.getAsJsonArray().get(0).getAsJsonObject().get("newClientState").getAsString();
        update.setNewClientState(clientState);
        update.setResponseType(ResponseType.FULL_UPDATE);
        final ListUpdateResponse[] responses = new ListUpdateResponse[] {update};
        response.setListUpdateResponses(responses);
        return response;
    }

    private Set<ThreatList> getThreatLists() {
        final Set<ThreatList> threatList = new HashSet<>();
        final ThreatList list = new ThreatList();
        list.setPlatformType(PlatformType.WINDOWS.name());
        list.setThreatEntryType(ThreatEntryType.URL.name());
        list.setThreatType(ThreatType.UNWANTED_SOFTWARE.name());
        threatList.add(list);
        return threatList;
    }

    @Test
    public void updateCacheFromFile() throws Exception {
        assertFalse("Hit on empty cache!", test.matches(TEST_URL).get());
        test.updateCache();
        assertTrue("Test URL not found!", test.matches(TEST_URL).get());
    }

    @Test
    public void updateCacheFromFile2() throws Exception {
        reset(updateApi);
        when(updateApi.loadThreatLists()).thenReturn(getThreatLists());
        when(updateApi.loadThreatListUpdates(any())).thenReturn(getThreatListUpdatesResponse(UNWANTED_SOFTWARE_WINDOWS_URL_TXT));
        updateCacheFromFile();
    }

    @Test
    public void updateCacheFromFile3() throws Exception {
        reset(updateApi);
        when(updateApi.loadThreatLists()).thenReturn(getThreatLists());
        when(updateApi.loadThreatListUpdates(any())).thenReturn(getThreatListUpdatesResponse(UNWANTED_SOFTWARE_WINDOWS_URL_TXT));
        final Collection<ThreatList> threatLists = Arrays.asList(new ThreatList());
        assertFalse("Result for unknown threat list!", test.matches(TEST_URL, threatLists).isPresent());
    }

}