package com.projectlounge.bean;

import com.projectlounge.BaseTest;
import com.projectlounge.utils.MutableUrl;
import org.junit.Test;

import javax.inject.Inject;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by main on 24.08.17.
 */
public class HashUtilsTest extends BaseTest {

    private static final String PATH = "src/test/resources/";
    private static final String TEST_URL = "http://up.mykings.pw:8888/update.txt";
    private static final String THREAT_LIST_TXT = "threatList.txt";
    private static final String DECODED_HASHES = "decodedHashes.txt";

    @Inject private HashUtils test;

    @Test
    public void testSha256Hash() throws Exception {
        sha256Hash("abc", "ba7816bf 8f01cfea 414140de 5dae2223 b00361a3 96177a9c b410ff61 f20015ad");
        sha256Hash("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq", "248d6a61 d20638b8 e5c02693 0c3e6039 a33ce459 64ff2167 f6ecedd4 19db06c1");
    }

    private void sha256Hash(final String input, final String hash) {
        final String expected = hash.replaceAll("\\s", "");
        final String actual = test.sha256Hex(input);
        final String message = String.format("Failed to make hash of prefix, input: '%s', result: '%s'", input, actual);
        assertEquals(message, expected, actual);
    }

    @Test
    public void testHashPrefix() throws Exception {
        final String input = "abc";
        final String expected = "ba78";
        final String actual = test.hashPrefix4(input);
        final String message = String.format("Failed to make hash of prefix, input: '%s', result: '%s'", input, actual);
        assertEquals(message, expected, actual);
    }

    @Test
    public void punycode() throws Exception {
        testPunycode("пример.испытание", "xn--e1afmkfd.xn--80akhbyknj4f");
        testPunycode("example.com", "example.com");
    }

    private void testPunycode(final String domain, final String expected) {
        final String actual = test.punyCode(domain);
        final String message = String.format("Punycode failed on '%s', result: '%s'", domain, actual);
        assertEquals(message, expected, actual);
    }

    @Test
    public void replaceWithPunyCode() throws Exception {
        testReplaceWithPunyCode("http://google.com/", "http://google.com/");
        testReplaceWithPunyCode("http://пример.испытание/", "http://xn--e1afmkfd.xn--80akhbyknj4f/");
        testReplaceWithPunyCode("http://пример.рф/?page=1", "http://xn--e1afmkfd.xn--p1ai/?page=1");
    }

    private void testReplaceWithPunyCode(final String url, final String expected) throws Exception {
        MutableUrl mutableUrl = new MutableUrl(url);
        test.convertToPunyCode(mutableUrl);
        final String actual = mutableUrl.getUrl();
        final String message = String.format("Failed to replace with punycode URL: '%s', result: '%s'", url, actual);
        assertEquals(message, expected, actual);
    }

    @Test
    public void testCanonicalize() throws Exception {
        final List<String> lines = Files.readAllLines(Paths.get(PATH + "canonicalization.txt"));
        for (String line : lines) {
            final String[] urls = line.split("\"");
            final String url = urls[1];
            final String expected = urls[3];
            final String actual = test.canonicalize(url).getUrl();
            final String message = String.format("Failed to canonicalize '%s' to '%s', result: '%s'", url, expected, actual);
            assertEquals(message, expected, actual);
        }
    }

    @Test
    public void removeFragment() throws Exception {
        testRemoveFragment("http://google.com/", "http://google.com/");
        testRemoveFragment("http://google.com/", "http://google.com/#frag");
        testRemoveFragment("http://google.com/page", "http://google.com/page#frag");
        testRemoveFragment("http://пример.испытание/", "http://пример.испытание/#frag");
    }

    private void testRemoveFragment(final String expected, final String url) {
        final MutableUrl mutableUrl = new MutableUrl(url);
        test.removeFragment(mutableUrl);
        final String actual = mutableUrl.getUrl();
        assertEquals(expected, actual);
    }

    @Test
    public void canonicalizeHostName() throws Exception {
        assertEquals("google.com", test.canonicalizeHostName("...google.com..."));
        assertEquals("google.com", test.canonicalizeHostName("google...com"));
        assertEquals("google.com", test.canonicalizeHostName("GOoGLe.cOm"));
    }

    @Test
    public void canonicalizePath() throws Exception {
        testCanonicalizePath("/", "/./");
        testCanonicalizePath("/", "/.//");
        testCanonicalizePath("/?p=1", "/./?p=1");

        testCanonicalizePath("/test", "/./test");
        testCanonicalizePath("/test/", "/./test/");
        testCanonicalizePath("/test/", "//.//test//");
        testCanonicalizePath("/path/test", "/path/./test");
        testCanonicalizePath("/path/test/?p=1", "/path/./test/?p=1");

        testCanonicalizePath("/", "/..");
        testCanonicalizePath("/", "/path/..");
        testCanonicalizePath("/", "/path/../");
        testCanonicalizePath("/?p=1", "/path/../?p=1");

        testCanonicalizePath("/", "/path/..//");
        testCanonicalizePath("/test", "/path/../test");
        testCanonicalizePath("/test/", "/path/..//test//");
        testCanonicalizePath("/test/?p=1", "/path/..//test//?p=1");

        testCanonicalizePath("/.secure", "/.secure");
    }

    private void testCanonicalizePath(final String expected, final String url) throws MalformedURLException {
        final String message = String.format("Failed to canonicalize path '%s' to '%s'", url, expected);
        assertEquals(message, expected, test.canonicalizePath(url));
    }

    @Test
    public void percentEscape() throws Exception {
        testPercentEscape("http://google.com/"+(char)12, "http://google.com/");
        testPercentEscape("http://google.com/"+(char)55, "http://google.com/7");
        testPercentEscape("http://google.com/"+(char)32, "http://google.com/");
        testPercentEscape("http://google.com/"+(char)33, "http://google.com/"+(char)33);
        testPercentEscape("http://google.com/"+(char)126, "http://google.com/"+(char)126);
        testPercentEscape("http://google.com/"+(char)127, "http://google.com/%7F");
        testPercentEscape("http://google.com/"+(char)128, "http://google.com/%80");
        testPercentEscape("http:// google.com/", "http://%20google.com/");
        testPercentEscape("http://\\x01\\x80.com/", "http://%01%80.com/");
        testPercentEscape("http://google.com/%", "http://google.com/%25");
    }

    private void testPercentEscape(final String url, final String expected) throws Exception {
        MutableUrl mutableUrl = new MutableUrl(url);
        test.percentEscape(mutableUrl);
        final String actual = mutableUrl.getUrl();
        final String message = String.format("Failed to percent escape URL: '%s', result: '%s'", url, actual);
        assertEquals(message, expected, actual);
    }

    @Test
    public void hostAndPath() throws Exception {
        testHostAndPath("http://google.com/q?r?s=2#frag", "google.com", "/q");
        testHostAndPath("http://google.com/path/q?r?s=2#frag", "google.com", "/path/q");
        testHostAndPath("http://google.com:80/path/q?r?s=2#frag", "google.com", "/path/q");
        testHostAndPath("http://google.com/#frag", "google.com", "/");
        testHostAndPath("http://google.com:80/#frag", "google.com", "/");
        testHostAndPath("http://google.com/", "google.com", "/");
        testHostAndPath("http://google.com:80/", "google.com", "/");
        testHostAndPath("http://google.com:80", "google.com", "/");
        testHostAndPath("http://google.com", "google.com", "/");
    }

    @Test
    public void changeHostAndPath() throws Exception {
        testChangeHost("http://www.google.com/q?r?s=2#frag", "www.yandex.ru", "http://www.yandex.ru/q?r?s=2#frag");
        testChangeHost("http://www.google.com:80/q?r?s=2#frag", "www.yandex.com", "http://www.yandex.com:80/q?r?s=2#frag");
        testChangePath("http://www.google.com/q?r?s=2#frag", "/newPath", "http://www.google.com/newPath?r?s=2#frag");
        testChangePath("http://www.google.com:80/q?r?s=2#frag", "/newPath", "http://www.google.com:80/newPath?r?s=2#frag");
        testChangePath("http://www.google.com:80/q?r?s=2#frag", "", "http://www.google.com:80/?r?s=2#frag");
    }

    private void testChangePath(final String url, final String path, final String expected) {
        final MutableUrl mutableUrl = new MutableUrl(url);
        mutableUrl.setPath(path);
        final String actual = mutableUrl.getUrl();
        final String message = String.format("Failed to change path: '%s', expected host: '%s', actual:'%s'", url, path, actual);
        assertEquals(message, expected, actual);
    }

    private void testChangeHost(final String url, final String host, final String expected) {
        final MutableUrl mutableUrl = new MutableUrl(url);
        mutableUrl.setHost(host);
        final String actual = mutableUrl.getUrl();
        final String message = String.format("Failed to change host: '%s', expected host: '%s', actual:'%s'", url, host, actual);
        assertEquals(message, expected, actual);
    }

    private void testHostAndPath(final String url, final String host, final String path) {
        final MutableUrl mutableUrl = new MutableUrl(url);
        final String actualHost = mutableUrl.getHost();
        final String actualPath = mutableUrl.getPath();
        final String messageHost = String.format("Failed to parse URL: '%s', expected host: '%s', actual:'%s'", url, host, actualHost);
        final String messagePath = String.format("Failed to parse URL: '%s', expected path: '%s', actual:'%s'", url, path, actualPath);
        assertEquals(messageHost, host, actualHost);
        assertEquals(messagePath, path, actualPath);
    }

    @Test
    public void escape() throws Exception {
        testEscape("%", "%25");
        testEscape("#", "%23");
    }

    private void testEscape(final String input, final String expected) {
        final String actual = test.escape(input);
        final String message = String.format("Failed to escape input: '%s', result: '%s", input, actual);
        assertEquals(message, expected, actual);
    }

    @Test
    public void parseIpAddress() throws Exception {
        String host = "3279880203";
        final String actual = test.parseIPAddress(host);
        final String message = String.format("Failed to parse IP Address '%s', result: '%s'", host, actual);
        assertEquals(message, "195.127.0.11", actual);
    }

    @Test
    public void removeSpecialCharacters() throws Exception {
        testRemoveSpecialCharacters("http://www.google.com/foo\tbar\rbaz\n2", "http://www.google.com/foobarbaz2");
    }

    private void testRemoveSpecialCharacters(final String url, final String expected) {
        final MutableUrl mutableUrl = new MutableUrl(url);
        test.removeSpecialCharacters(mutableUrl);
        final String actual = mutableUrl.getUrl();
        final String message = String.format("Failed to remove special characters from url: '%s', result: '%s'", url, actual);
        assertEquals(message, expected, actual);
    }

    @Test
    public void isIpAddress() throws Exception {
        testIsIpAddress("", false);
        testIsIpAddress("google.com", false);
        testIsIpAddress("256.8.8.8", false);
        testIsIpAddress("127.0.0.1", true);
        testIsIpAddress("192.168.0.1", true);
        testIsIpAddress("8.8.8.8", true);
        testIsIpAddress("255.255.255.255", true);
        testIsIpAddress("::ffff:192.0.2.1", true);
        testIsIpAddress("2001:0db8:11a3:09d7:1f34:8a2e:07a0:765d", true);
        testIsIpAddress("::", true);
        testIsIpAddress("::1", true);
    }

    private void testIsIpAddress(final String ip, final boolean expected) {
        final boolean actual = test.isIpAddress(ip);
        final String message = String.format("Failed to check IP address string '%s', result '%b'", ip, actual);
        assertEquals(message, expected, actual);
    }

    @Test
    public void createPrefix() throws Exception {
        testCreatePrefix("www.google.com", "www.google.com", "google.com");
        testCreatePrefix("1.2.3.4.com","1.2.3.4.com", "2.3.4.com", "3.4.com", "4.com");
        testCreatePrefix("1.2.3.4.5.com","1.2.3.4.5.com", "2.3.4.5.com", "3.4.5.com", "4.5.com", "5.com");
        testCreatePrefix("1.2.3.4.5.6.com","1.2.3.4.5.6.com", "3.4.5.6.com", "4.5.6.com", "5.6.com", "6.com");
    }

    private void testCreatePrefix(final String host, final String... parts) {
        final Collection<String> expected = Arrays.asList(parts);
        final Collection<String> actual = test.createPrefix(host);
        final String message = String.format("Failed to create prefix for '%s', result: '%s'", host, actual);
        assertTrue(message, expected.containsAll(actual));
        assertEquals(message, expected.size(), actual.size());
    }

    @Test
    public void createSuffix() throws Exception {
        testCreateSuffix("/1/2.html", "?param=1", "/", "/1/2.html?param=1", "/1/2.html", "/1/");
        testCreateSuffix("/path/to/page", "?p=1&p=2", "/", "/path/to/page?p=1&p=2", "/path/to/page", "/path/to/", "/path/");
        testCreateSuffix("/1/2/3/4/5.html", "?p", "/", "/1/2/3/4/5.html", "/1/2/3/4/5.html?p", "/1/2/3/4/", "/1/2/3/", "/1/2/", "/1/");
    }

    private void testCreateSuffix(final String path, final String query, final String... parts) {
        final Collection<String> expected = Arrays.asList(parts);
        final Collection<String> actual = test.createSuffix(path, query);
        final String message = String.format("Failed to create suffix for '%s%s', result: '%s'", path, query, actual);
        assertTrue(message, expected.containsAll(actual));
        assertEquals(message, expected.size(), actual.size());
    }

    @Test
    public void createSuffixPrefix() throws Exception {
        testCreateSuffixPrefix("http://a.b.c.d.e.f.g/1.html",
                "a.b.c.d.e.f.g/1.html",
                "a.b.c.d.e.f.g/",
                "c.d.e.f.g/1.html",
                "c.d.e.f.g/",
                "d.e.f.g/1.html",
                "d.e.f.g/",
                "e.f.g/1.html",
                "e.f.g/",
                "f.g/1.html",
                "f.g/"
        );
        testCreateSuffixPrefix("http://a.b.c/1/2.html?param=1",
                "a.b.c/1/2.html?param=1",
                "a.b.c/1/2.html",
                "a.b.c/",
                "a.b.c/1/",
                "b.c/1/2.html?param=1",
                "b.c/1/2.html",
                "b.c/",
                "b.c/1/"
        );
        testCreateSuffixPrefix("http://1.2.3.4/1/",
                "1.2.3.4/1/",
                "1.2.3.4/"
        );
    }

    private void testCreateSuffixPrefix(final String url, final String... expected) {
        final List<String> expectedList = Arrays.asList(expected);
        final Set<String> actual = test.createSuffixPrefixExpressions(new MutableUrl(url));
        for (String each : expectedList) {
            final String message = String.format("Failed to create suffix/prefix for '%s' expected '%s' not in result: '%s", url, each, actual);
            assertTrue(message, actual.contains(each));
        }
        final String message = String.format("Failed to create suffix prefix expr: actual size is not expected, '%s'", actual);
        assertEquals(message, expectedList.size(), actual.size());
        assertTrue("Failed to create suffix/prefix expr, expected 30 at most", actual.size() <= 30);
    }

    @Test
    public void hashesTest() throws Exception {
        final List<byte[]> fullHashes = test.makeHashesBinary(TEST_URL);
        final byte[] allHashes = Files.readAllBytes(Paths.get(PATH + DECODED_HASHES));
        for (int i = 0; i < allHashes.length; i = i + 4) {
            final byte[] hash = new byte[4];
            System.arraycopy(allHashes, i, hash, 0, 4);
            for (byte[] fullHash : fullHashes) {
                final byte[] hashPrefix = Arrays.copyOf(fullHash, 4);
                if (Arrays.equals(hash, hashPrefix)) {
                    return;
                }
            }
        }
        fail("Hash not found!");
    }

    @Test
    public void hashesNegativeTest() throws Exception {
        final List<byte[]> fullHashes = test.makeHashesBinary("http://www.google.com");
        final byte[] allHashes = Files.readAllBytes(Paths.get(PATH + DECODED_HASHES));
        for (int i = 0; i < allHashes.length; i = i + 4) {
            final byte[] hash = new byte[4];
            System.arraycopy(allHashes, i, hash, 0, 4);
            for (byte[] fullHash : fullHashes) {
                final byte[] hashPrefix = Arrays.copyOf(fullHash, 4);
                if (Arrays.equals(hash, hashPrefix)) {
                    fail("Hash for safe URL has been found!");
                }
            }
        }
    }

//    @Test
//    public void fullHashesTest() throws Exception {
//        final List<byte[]> fullHashes = test.makeHashesBinary(TEST_URL);
//        final String api = "https://safebrowsing.googleapis.com/v4/fullHashes:find?key=" + apiKey;
//
//        final com.projectlounge.utils.UtilsTest.FullHashesRequest request = new com.projectlounge.utils.UtilsTest.FullHashesRequest();
//        request.setClient(createClient());
//        request.setApiClient(createClient());
//        final Map<String, Object> threatInfo = new HashMap<>();
//        threatInfo.put("threatTypes", new String[] {"MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE", "POTENTIALLY_HARMFUL_APPLICATION", "THREAT_TYPE_UNSPECIFIED"});
//        threatInfo.put("platformTypes", new String[] {"WINDOWS"});
//        threatInfo.put("threatEntryTypes", new String[] {"URL"});
//        final Object[] threatEntries = new Object[fullHashes.size()];
//        int i = 0;
//        for (byte[] fullHash : fullHashes) {
//            final Map<String, String> map = new HashMap<>();
//            map.put("url", TEST_URL);
//            map.put("hash", Base64.getEncoder().encodeToString(fullHash));
//            threatEntries[i++] = map;
//        }
//        threatInfo.put("threatEntries", threatEntries);
//        request.setThreatInfo(threatInfo);
//
//        final RestTemplate rest = new RestTemplate();
//        final ResponseEntity<String> entity = rest.postForEntity(api, request, String.class);
//        final String body = entity.getBody();
//        JsonElement element = new JsonParser().parse(body);
//        final JsonElement matches = element.getAsJsonObject().get("matches").getAsJsonArray().get(0);
//        final JsonElement threat = matches.getAsJsonObject().get("threat");
//        final JsonElement hash = threat.getAsJsonObject().get("hash");
//        final byte[] hashBinary = Base64.getDecoder().decode(hash.getAsString());
//        for (byte[] fullHash : fullHashes) {
//            if (Arrays.equals(fullHash, hashBinary)) {
//                return;
//            }
//        }
//        fail("Full hash not found!");
//    }

//    @Test
//    public void lookupTest() throws Exception {
//        final String api = "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=" + apiKey;
//
//        final com.projectlounge.utils.UtilsTest.LookupRequest request = new com.projectlounge.utils.UtilsTest.LookupRequest();
//        request.setClient(createClient());
//        request.setThreatInfo(createThreatInfo());
//
//        final RestTemplate rest = new RestTemplate();
//        final ResponseEntity<String> entity = rest.postForEntity(api, request, String.class);
//        final String body = entity.getBody();
//        assertTrue(body.contains(TEST_URL));
//        JsonElement element = new JsonParser().parse(body);
//        final JsonElement matches = element.getAsJsonObject().get("matches").getAsJsonArray().get(0);
//        final JsonElement threat = matches.getAsJsonObject().get("threat");
//        final JsonElement url = threat.getAsJsonObject().get("url");
//        assertEquals("Failed to test url!", TEST_URL, url.getAsString());
//    }
//
//    private Map<String, Object> createThreatInfo() {
//        final Map<String, Object> threatInfo = new HashMap<>();
//        threatInfo.put("threatTypes", new String[] {"MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE", "POTENTIALLY_HARMFUL_APPLICATION", "THREAT_TYPE_UNSPECIFIED"});
//        threatInfo.put("platformTypes", new String[] {"WINDOWS"});
//        threatInfo.put("threatEntryTypes", new String[] {"URL"});
//        final Map<String,String> urlMap = new HashMap<>();
//        urlMap.put("url", TEST_URL);
//        threatInfo.put("threatEntries", new Object[]{urlMap});
//        return threatInfo;
//    }
//
//    private Map<String, String> createClient() {
//        final Map<String, String> client = new HashMap<>();
//        client.put("clientId", "My test company");
//        client.put("clientVersion", "1.5.2");
//        return client;
//    }
//
//    @JsonIgnoreProperties(ignoreUnknown = true)
//    @Data
//    private static class FullHashesRequest {
//        private Map<String, String> client;
//        private String[] clientStates;
//        private Map<String, Object> threatInfo;
//        private Map<String, String> apiClient;
//    }
//
//    @JsonIgnoreProperties(ignoreUnknown = true)
//    @Data
//    private static class TreatListRequest {
//        private Map<String, String> client;
//        private Object[] listUpdateRequests;
//    }
//
//    @JsonIgnoreProperties(ignoreUnknown = true)
//    @Data
//    private static class LookupRequest {
//        private Map<String, String> client;
//        private Map<String, Object> threatInfo;
//    }


}