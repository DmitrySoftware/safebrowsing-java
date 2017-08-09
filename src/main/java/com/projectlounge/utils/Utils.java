package com.projectlounge.utils;

import com.google.common.net.InetAddresses;
import com.uri.idn.PunycodeException;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.UnsupportedEncodingException;
import java.net.IDN;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by main on 08.08.17.
 */
public class Utils {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Utils.class.getName());

    public static String hash(final String url) throws URISyntaxException, PunycodeException, MalformedURLException, UnsupportedEncodingException {
        // todo implement
        return canonicalize(url);
    }

    public static String hashPrefix(final String input) {
        final String hash = sha256Hex(input);
        // todo implement
        return hash;
    }

    public static String sha256Hex(final String input) {
        return DigestUtils.sha256Hex(input);
    }

    public static String canonicalize(final String input) throws URISyntaxException, PunycodeException, MalformedURLException, UnsupportedEncodingException {
        final URI uri = new URI(input);
        String urlStr = convertToPunyCode(uri);
        urlStr = removeSpecialCharacters(urlStr);
        urlStr = removeFragment(urlStr);
        urlStr = percentUnescape(urlStr);
        urlStr = canonicalizeHostAndPath(urlStr);
//        urlStr = percentEscape(urlStr); //todo fix
        urlStr = addTrailingSlash(urlStr);
        return urlStr;
    }

    private static String canonicalizeHostAndPath(final String url) throws MalformedURLException, URISyntaxException {
        final URI uri = new URI(url);
        final String host = canonicalizeHostName(uri.getHost());
        final String path = canonicalizePath(uri.getPath());
        return url(uri, host, path);
    }

    public static String percentUnescape(final String s)  {
        final int len = s.length();
        if (len < 3) return s;
        final StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len -2; i++) {
            final char cp = s.charAt(i);
            if (cp=='%' && isHex(s.charAt(i+1)) && isHex(s.charAt(i+2))) {
                final int unHex = Integer.parseInt(s.substring(i + 1, i + 3));
                if (unHex<32 || unHex>128) {
                    sb.append((char) unHex);
                }
                i = i + 2;
            } else {
                sb.appendCodePoint(cp);
            }
        }
        return sb.toString();
    }

    public static boolean isHex(final int cp) {
        return '0'<=cp && cp<='9'
            || 'a'<=cp && cp<='f'
            || 'A'<=cp && cp<='F';
    }

    public static int unHex(final char c) {
        if ('0'<=c && c<='9') return c - '0';
        if ('a'<=c && c<='f') return c - 'a' + 10;
        if ('A'<=c && c<='F') return c - 'A' + 10;
        return 0;
    }

    private static String removeSpecialCharacters(final String urlString) {
        return urlString.replaceAll("([\\r\\n\\t])", "");
    }

    private static String addTrailingSlash(final String urlString) throws MalformedURLException {
        final String path = new URL(urlString).getPath();
        return path.trim().isEmpty() ? urlString + '/' : urlString;
    }

    public static String percentEscape(final String url) throws URISyntaxException, MalformedURLException {
        final StringBuilder sb = new StringBuilder(url.length());
        for (int i=0; i<url.length(); i++) {
            final char cp = url.charAt(i);
            if (cp<=32 || cp>=128 || Character.isWhitespace(cp) || cp=='%' || cp=='#') {
                sb.append(String.format("%%%02X", (int)cp));
            } else {
                sb.append(cp);
            }
        }
        return sb.toString();
    }

    public static String canonicalizePath(final String path) throws MalformedURLException {
        final String regex1 = "/?[^/]*/\\.\\./?"; // removing "/../" along with the preceding path component
        final String regex2 = "/\\./?"; // replacing "/./" with "/"
        final String regex3 = "/{2,}"; // replace runs of consecutive slashes with a single slash character
        return path.replaceAll(regex1 + "|" + regex2, "/").replaceAll(regex3, "/");
    }

    public static String canonicalizeHostName(final String host) throws MalformedURLException {
        String result = host.replaceAll("^\\.+|\\.+$", ""); // remove all leading and trailing dots
        result = result.replaceAll("\\.+", "."); // replace consecutive dots with a single dot
        result = normalizeIpAddress(result);
        result = result.toLowerCase(); // lowercase the whole string
        return result;
    }

    private static String normalizeIpAddress(final String host) {
        if (!InetAddresses.isInetAddress(host)) return host;
        //todo normalize ip address
        return host;
    }

    public static String removeFragment(final String url) {
        final int i = url.indexOf('#');
        if (i < 0) return url;
        return url.substring(0, i);
    }

    public static String convertToPunyCode(final URI uri) throws MalformedURLException, URISyntaxException {
        final String host = getHost(uri);
        final String punyCode = punyCode(host);
        if (punyCode.equals(host)) return uri.toString();
        return url(uri, punyCode, uri.getPath());
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

}
