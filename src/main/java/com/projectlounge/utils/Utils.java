package com.projectlounge.utils;

import jersey.repackaged.com.google.common.collect.Lists;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.validator.routines.InetAddressValidator;

import java.net.IDN;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by main on 08.08.17.
 */
public class Utils {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Utils.class.getName());

    // todo add other patterns
    private static final Pattern TRAILING_SPACE_REGEXP = Pattern.compile("^(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})");
    private static final Pattern POSSIBLE_IP_REGEXP = Pattern.compile("^(?i)((?:0x[0-9a-f]+|[0-9\\.])+)$");

    public static String hash(final String input)  {
        final MutableUrl url = canonicalize(input);
        final Set<String> expressions = createSuffixPrefixExpressions(url);
        return url.toString();
    }

    private static Set<String> createSuffixPrefixExpressions(final MutableUrl url) {
        final Collection<String> prefix = createPrefix(url.getHost());
        final Set<String> result = new HashSet<>(30);
        return result;
    }

    public static Collection<String> createPrefix(final String host) {
        final Set<String> result = new HashSet<>(5);
        result.add(host);
        boolean ipAddress = isIpAddress(host);
        if (ipAddress) return result;
        final String[] split = host.split("\\.");
        final int size = Math.min(split.length, 5);
        final int from = size > 5 ? split.length - 5 : 0;
        final List<String> list = Arrays.asList(split);
        for (int i = from; i < size-1; i++) {
            final String component = String.join(".", list.subList(i, split.length));
            result.add(component);
        }
        return result;
    }

    public static boolean isIpAddress(final String host) {
        return InetAddressValidator.getInstance().isValid(host);
    }

    public static Collection<String> makeSuffixPrefix(final String host, final String path, final String params) {
        final List<String> result = new ArrayList<>();
        result.add(host + path + params); // The exact path of the URL, including query parameters.
        result.add(host + path); // The exact path of the URL, without query parameters.
        final String[] pathComponents = path.split("/");
        result.add(host + path + params);
        result.add(host + path);
        result.add(host + '/');
        return result;
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

    public static MutableUrl canonicalize(final String input) {
        final MutableUrl url = new MutableUrl(input);
        makeValid(url);
        convertToPunyCode(url);
        removeSpecialCharacters(url);
        removePort(url);
        removeFragment(url);
        percentUnescape(url);
        canonicalizeHostAndPath(url);
        percentEscape(url);
        return url;
    }

    private static void makeValid(final MutableUrl url) {
        final String host = url.getHost()
                .replaceAll("^\\.+|\\.+$", "") // remove all leading and trailing dots
                .replaceAll("[.]+", "."); // replace consecutive dots with a single dot
        url.setHost(host);
    }

    public static void removePort(final MutableUrl url) {
        url.setPort("");
    }

    private static void canonicalizeHostAndPath(final MutableUrl url) {
        final String host = canonicalizeHostName(url.getHost());
        url.setHost(host);
        final String path = canonicalizePath(url.getPath());
        url.setPath(path);
    }

    public static void percentUnescape(final MutableUrl url) {
        final String path = unescape(url.getPath());
        url.setPath(path);
        final String host = unescape(url.getHost());
        url.setHost(host);
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
                final String valueStr = s.substring(i+1, i+3);
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

    public static void removeSpecialCharacters(final MutableUrl url) {
        final String input = url.getUrl();
        final String result = input.replaceAll("([\\r\\n\\t]|\\\\t|\\\\n|\\\\r)", "");
        url.setUrl(result);
    }

    public static void percentEscape(final MutableUrl url) {
        final String host = escape(url.getHost());
        url.setHost(host);
        final String path = escape(url.getPath());
        url.setPath(path);
    }

    public static String escape(final String url) {
        final StringBuilder sb = new StringBuilder(url.length());
        for (int i = 0; i < url.length(); i++) {
            final char c = url.charAt(i);
            if (c=='\\' && i<url.length()+1) {
                if (url.charAt(i+1) == 'x') sb.append('%');
                i++;
            } else if (!Character.isWhitespace(c) && c!='%' && c!='#' && c>=31 && c<=126) {
                sb.append(c);
            } else {
                sb.append(String.format("%%%02X", (int) c));
            }
        }
        return sb.toString();
    }

    public static String canonicalizePath(final String path) {
        final String regex1 = "/?[^/]*/\\.\\./?"; // removing "/../" along with the preceding path component
        final String regex2 = "/[.]/"; // replacing "/./" with "/"
        final String regex3 = "/+"; // replace runs of consecutive slashes with a single slash character
        return path.replaceAll(regex1 + "|" + regex2, "/").replaceAll(regex3, "/");
    }

    public static String canonicalizeHostName(final String host) {
        String result = host.replaceAll("^[.]+|[.]+$", ""); // remove all leading and trailing dots
        result = result.replaceAll("[.]+", "."); // replace consecutive dots with a single dot
        result = normalizeIpAddress(result);
        result = result.toLowerCase(); // lowercase the whole string
        return result;
    }

    private static String normalizeIpAddress(final String host) {
        return parseIPAddress(host);
    }

    public static void removeFragment(final MutableUrl mutableUrl) {
        final String url = mutableUrl.getUrl();
        final int i = url.indexOf('#');
        if (i < 0) return;
        mutableUrl.setUrl(url.substring(0, i));
    }

    public static void convertToPunyCode(final MutableUrl url) {
        final String host = url.getHost();
        final String punyCode = punyCode(host);
        if (punyCode.equals(host)) return;
        url.setHost(punyCode);
    }

    public static String punyCode(final String domain) {
        return IDN.toASCII(domain);
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
