package com.projectlounge.utils;

import com.uri.idn.PunycodeException;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.UnsupportedEncodingException;
import java.net.IDN;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by main on 08.08.17.
 */
public class Utils {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Utils.class.getName());

    private static final Pattern TRAILING_SPACE_REGEXP = Pattern.compile("^(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})");
    private static final Pattern POSSIBLE_IP_REGEXP = Pattern.compile("^(?i)((?:0x[0-9a-f]+|[0-9\\.])+)$");

    public static String hash(final String url) throws URISyntaxException, PunycodeException, MalformedURLException, UnsupportedEncodingException {
        // todo implement
        return canonicalize(url);
    }

    public static String hashPrefix4(final String input) {
        return hashPrefix(input, 4);
    }

    public static String hashPrefix(final String input, final int size) {
        final String hash = sha256Hex(input);
        return hash.substring(0, size);
    }

    public static String sha256Hex(final String input) {
        return DigestUtils.sha256Hex(input);
    }

    public static String canonicalize(final String input) throws URISyntaxException, PunycodeException, MalformedURLException, UnsupportedEncodingException {
        String url = convertToPunyCode(input);
        url = addTrailingSlash(url); //todo pass mutable url
        url = removeSpecialCharacters(url);
        url = removePort(url);
        url = removeFragment(url);
        url = percentUnescape(url);
        url = canonicalizeHostAndPath(url);
        url = percentEscape(url);
        return url;
    }

    public static String removePort(final String url) {
        final MutableUrl mutableUrl = new MutableUrl(url);
        mutableUrl.removePort();
        return mutableUrl.getUrl();
    }

    private static String canonicalizeHostAndPath(final String url) throws MalformedURLException, URISyntaxException {
        final MutableUrl mutableUrl = new MutableUrl(url);
        final String host = canonicalizeHostName(mutableUrl.getHost());
        final String path = canonicalizePath(mutableUrl.getPath());
        mutableUrl.setHost(host);
        mutableUrl.setPath(path);
        return mutableUrl.getUrl();
    }

    public static String percentUnescape(final String s) throws UnsupportedEncodingException {
        final MutableUrl url = new MutableUrl(s);
        final String path = unescape(url.getPath());
        final String host = unescape(url.getHost());
        url.setPath(path);
        url.setHost(host);
        return url.getUrl();
    }

    private static String unescape(String path) {
        int i = 0;
        while (i < 1024 && !Objects.equals(path, path = doUnescape(path))) { //todo simplify
            i++;
        }
        if (i == 1024) throw new IllegalArgumentException("");// todo name
        return path;
    }

    private static String doUnescape(final String s) {
        final int len = s.length();
        if (len < 3) return s;
        final int end = len - 2;
        final StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < len; i++) {
            final char c = s.charAt(i);
            if (c == '%' && i < end && isHex(s.charAt(i + 1)) && isHex(s.charAt(i + 2))) {
                final String valueStr = s.substring(i + 1, i + 3);
                final char hex = (char) Integer.parseInt(valueStr, 16);
                sb.append(hex);
                i = i + 2;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static boolean isHex(final char c) {
        return '0' <= c && c <= '9'
                || 'a' <= c && c <= 'f'
                || 'A' <= c && c <= 'F';
    }

    public static int unHex(final char c) {
        if ('0' <= c && c <= '9') return c - '0';
        if ('a' <= c && c <= 'f') return c - 'a' + 10;
        if ('A' <= c && c <= 'F') return c - 'A' + 10;
        return 0;
    }

    private static String removeSpecialCharacters(final String urlString) {
        return urlString.replaceAll("([\\r\\n\\t])", "");
    }

    public static String addTrailingSlash(final String urlString) throws MalformedURLException {
        final MutableUrl url = new MutableUrl(urlString);
        url.fixPath();
        return url.getUrl();
    }

    public static String percentEscape(final String url) throws URISyntaxException, MalformedURLException, UnsupportedEncodingException {
        final MutableUrl mutableUrl = new MutableUrl(url);
        final String path = escape(mutableUrl.getPath());
        final String host = escape(mutableUrl.getHost());
        mutableUrl.setPath(path);
        mutableUrl.setHost(host);
        return mutableUrl.getUrl();
    }

    public static String escape(final String url) {
        final StringBuilder sb = new StringBuilder(url.length());
        for (int i = 0; i < url.length(); i++) {
            final char c = url.charAt(i);
            if (!Character.isWhitespace(c) && c != '%' && c != '#' && c > 32 && c < 128) {
                sb.append(c);
            } else {
                sb.append(String.format("%%%02X", (int) c));
            }
        }
        return sb.toString();
    }

    public static String canonicalizePath(final String path) throws MalformedURLException {
        final String regex1 = "/?[^/]*/\\.\\./?"; // removing "/../" along with the preceding path component
        final String regex2 = "/\\./"; // replacing "/./" with "/"
        final String regex3 = "/{2,}"; // replace runs of consecutive slashes with a single slash character
        return path.replaceAll(regex1 + "|" + regex2, "/").replaceAll(regex3, "/");
    }

    public static String canonicalizeHostName(final String host) throws MalformedURLException {
        String result = host.replaceAll("^\\.+|\\.+$", ""); // remove all leading and trailing dots
        result = result.replaceAll("\\.{2,}+", "."); // replace consecutive dots with a single dot
        result = normalizeIpAddress(result);
        result = result.toLowerCase(); // lowercase the whole string
        return result;
    }

    private static String normalizeIpAddress(final String host) {
        return parseIPAddress(host);
    }

    public static String removeFragment(final String url) {
        final int i = url.indexOf('#');
        if (i < 0) return url;
        return url.substring(0, i);
    }

    public static String convertToPunyCode(final String url) throws MalformedURLException, URISyntaxException {
        final MutableUrl mutableUrl = new MutableUrl(url);
        final String host = mutableUrl.getHost();
        final String punyCode = punyCode(host);
        if (punyCode.equals(host)) return url;
        mutableUrl.setHost(punyCode);
        return mutableUrl.getUrl();
    }

    private static String getHost(final URI uri) throws MalformedURLException {
        return uri.toURL().getHost();
    }

    public static String punyCode(final String domain) {
        return IDN.toASCII(domain);
    }

    private static URI uri(final URI uri, final String host, final String path) throws URISyntaxException {
        return new URI(uri.getScheme(), uri.getUserInfo(), host, uri.getPort(), path, uri.getQuery(), uri.getFragment());
    }

    private static String url(final URI uri, final String host, final String path) throws URISyntaxException {
        return uri(uri, host, path).toString();
    }

    public static int findFragmentIndex(final String url, final int pathIndex) {
        int i = url.length();
        while (--i > pathIndex) {
            final char c = url.charAt(i);
            if (c == '#') return i;
            if (!Character.isLetterOrDigit(c)) return -1;
        }
        return -1;
    }

    /**
     * The Windows resolver allows a 4-part dotted decimal IP address to have a
     * space followed by any old rubbish, so long as the total length of the
     * string doesn't get above 15 characters. So, "10.192.95.89 xy" is
     * resolved to 10.192.95.89. If the string length is greater than 15
     * characters, e.g. "10.192.95.89 xy.wildcard.example.com", it will be
     * resolved through DNS.
     * @param host hostname
     * @return ip address string
     */
    public static String parseIPAddress(String host) {
        if (host.length() <= 15) {
            final Matcher matcher = TRAILING_SPACE_REGEXP.matcher(host);
            if (matcher.matches()) {
                host = host.trim();
            }
        }
        final Matcher matcher = POSSIBLE_IP_REGEXP.matcher(host);
        if (!matcher.matches()) {
            return host;
        }
        final String[] parts = host.split("\\.");
        if (parts.length > 4) {
            return host;
        }
        for (int i = 0; i < parts.length; i++) {
            final String part = parts[i];
            if (i == parts.length-1) {
                parts[i] = canonicalNum(part, 5 - parts.length);
            } else {
                parts[i] = canonicalNum(part, 1);
            }
            if (part.isEmpty()) return host;
        }
        return String.join(".", parts);
    }

    /**
     * canonicalNum parses s as an integer and attempts to encode it as a '.'
     * separated string where each element is the base-10 encoded value of each byte
     * for the corresponding number, starting with the MSB. The result is one that
     * is usable as an IP address.
     * <br/><br/>
     * For example: <br/>
     *	s:"01234",      n:2  =>  "2.156" <br/>
     *	s:"0x10203040", n:4  =>  "16.32.48.64"
     * @param s input
     * @param n number
     * @return ip address string
     */
    private static String canonicalNum(final String s, final int n) {
        if (n<=0 || n>4) return "";
        Long v = parseLong(s);
        if (null==v) return "";
        final String[] result = new String[n];
        for (int i = n - 1; i >= 0; i--) {
            result[i] = String.valueOf(v & 0xff);
            v = v >> 8;
        }
        return String.join(".", result);
    }

    private static Long parseLong(final String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
