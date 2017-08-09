package com.projectlounge.utils;

import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by main on 08.08.17.
 */
public class UtilsTest {

    private static final String PATH = "src/test/resources/";

    @Test
    public void hash() throws Exception {

    }

    @Test
    public void testSha256Hash() throws Exception {
        sha256Hash("abc", "ba7816bf 8f01cfea 414140de 5dae2223 b00361a3 96177a9c b410ff61 f20015ad");
        sha256Hash("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq", "248d6a61 d20638b8 e5c02693 0c3e6039 a33ce459 64ff2167 f6ecedd4 19db06c1");
    }

    private void sha256Hash(final String input, final String hash) {
        final String expected = hash.replaceAll("\\s", "");
        final String actual = Utils.sha256Hex(input);
        final String message = String.format("Failed to make hash of prefix, input: '%s', result: '%s'", input, actual);
        assertEquals(message, expected, actual);
    }

    @Test
    public void testHashPrefix() throws Exception {
        final String input = "abc";
        final String expected = "ba7816bf";
        final String actual = Utils.hashPrefix(input);
        final String message = String.format("Failed to make hash of prefix, input: '%s', result: '%s'", input, actual);
        assertEquals(message, expected, actual);
    }

    @Test
    public void name() throws Exception {
        final String s = Utils.hashPrefix("abc");
        final String prefix = s.substring(0, 8);
        System.out.println(prefix);
    }

    @Test
    public void punycode() throws Exception {
        testPunycode("пример.испытание", "xn--e1afmkfd.xn--80akhbyknj4f");
        testPunycode("example.com", "example.com");
    }

    private void testPunycode(final String domain, final String expected) {
        final String actual = Utils.punyCode(domain);
        final String message = String.format("Punycode failed on '%s', result: '%s'", domain, actual);
        assertEquals(message, expected, actual);
    }

    @Test
    public void replaceWithPunyCode() throws Exception {
        testReplaceWithPunyCode("http://google.com/", "http://google.com/");
        testReplaceWithPunyCode("http://пример.испытание/", "http://xn--e1afmkfd.xn--80akhbyknj4f/");
        testReplaceWithPunyCode("http://пример.рф/?page=1", "http://xn--e1afmkfd.xn--p1ai/?page=1");
    }

    private void testReplaceWithPunyCode(final String href, final String expected) throws Exception {
        final URI uri = new URI(href);
        final String actual = Utils.convertToPunyCode(uri);
        final String message = String.format("Failed to replace with punycode URL: '%s', result: '%s'", href, actual);
        assertEquals(message, expected, actual);
    }

    @Test
    public void canonicalize() throws Exception {
        final List<String> lines = Files.readAllLines(Paths.get(PATH + "canonicalization.txt"));
        for (String line : lines) {
            final String[] urls = line.split("\"");
            final String url = urls[1];
            final String expected = urls[3];
            final String actual = Utils.canonicalize(url);
            final String message = String.format("Failed to canonicalize '%s' to '%s'", url, expected);
            assertEquals(message, expected, actual);
        }
    }

    @Test
    public void removeFragment() throws Exception {
        assertEquals("http://google.com/", Utils.removeFragment("http://google.com/"));
        assertEquals("http://google.com/", Utils.removeFragment("http://google.com/#frag"));
        assertEquals("http://пример.испытание/", Utils.removeFragment("http://пример.испытание/#frag"));
    }

    @Test
    public void canonicalizeHostName() throws Exception {
        assertEquals("google.com", Utils.canonicalizeHostName("...google.com..."));
        assertEquals("google.com", Utils.canonicalizeHostName("google...com"));
        assertEquals("google.com", Utils.canonicalizeHostName("GOoGLe.cOm"));
    }

    @Test
    public void canonicalizePath() throws Exception {
        testCanonicalizePath("/", "/.");
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
    }

    private void testCanonicalizePath(final String expected, final String url) throws MalformedURLException {
        final String message = String.format("Failed to canonicalize path '%s' to '%s'", url, expected);
        assertEquals(message, expected, Utils.canonicalizePath(url));
    }

    @Test
    public void percentEscape() throws Exception {
        testPercentEscape("http://google.com/"+(char)12, "http://google.com/%0C");
        testPercentEscape("http://google.com/"+(char)32, "http://google.com/%20");
        testPercentEscape("http://google.com/"+(char)128, "http://google.com/%80");
        testPercentEscape("http://google.com/"+(char)55, "http://google.com/7");
        testPercentEscape("http://google.com/ ", "http://google.com/%20");
        testPercentEscape("http://google.com/#", "http://google.com/%23");
        testPercentEscape("http://google.com/%", "http://google.com/%25");
    }

    private void testPercentEscape(final String url, final String expected) throws Exception {
        final String actual = Utils.percentEscape(url);
        final String message = String.format("Failed to percent escape URL: '%s', result: '%s'", url, actual);
        assertEquals(message, expected, actual);
    }

    @Test
    public void unHex() throws Exception {
        final String test = "CAFE";
        assertEquals("c", Integer.toHexString(Utils.unHex(test.charAt(0))));
        assertEquals("a", Integer.toHexString(Utils.unHex(test.charAt(1))));
        assertEquals("f", Integer.toHexString(Utils.unHex(test.charAt(2))));
        assertEquals("e", Integer.toHexString(Utils.unHex(test.charAt(3))));
    }
}