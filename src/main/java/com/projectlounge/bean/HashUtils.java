package com.projectlounge.bean;

import com.projectlounge.utils.MutableUrl;
import lombok.Getter;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.springframework.stereotype.Component;

import java.net.IDN;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by main on 24.08.17.
 */
@Component
@Getter
public class HashUtils {

    private static final ThreadLocal<Pattern> TRAILING_SPACE_REGEXP = ThreadLocal.withInitial(
            () -> Pattern.compile("^(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})")
    );
    private static final ThreadLocal<Pattern> POSSIBLE_IP_REGEXP = ThreadLocal.withInitial(
            () -> Pattern.compile("^(?i)((?:0x[0-9a-f]+|[0-9\\.])+)$")
    );
    private static final ThreadLocal<Pattern> LEADING_TRAILING_DOTS = ThreadLocal.withInitial(
            () -> Pattern.compile("^\\.+|\\.+$")
    );
    private static final ThreadLocal<Pattern> CONSECUTIVE_DOTS = ThreadLocal.withInitial(
            () -> Pattern.compile("[.]+")
    );
    private static final ThreadLocal<Pattern> SPECIAL_CHARACTERS = ThreadLocal.withInitial(
            () -> Pattern.compile("([\\r\\n\\t]|\\\\t|\\\\n|\\\\r)")
    );
    /** removing "/../" along with the preceding path component and replacing "/./" with "/" */
    private static final ThreadLocal<Pattern> CANONICAL_PATH_1 = ThreadLocal.withInitial(
            () -> Pattern.compile("(/?[^/]*/\\.\\./?)|(/[.]/)")
    );
    /** replace runs of consecutive slashes with a single slash character */
    private static final ThreadLocal<Pattern> CANONICAL_PATH_2 = ThreadLocal.withInitial(
            () -> Pattern.compile("/+")
    );
    /** remove all leading and trailing dots */
    private static final ThreadLocal<Pattern> CANONICAL_HOST_1 = ThreadLocal.withInitial(
            () -> Pattern.compile("^[.]+|[.]+$")
    );
    /** replace consecutive dots with a single dot */
    private static final ThreadLocal<Pattern> CANONICAL_HOST_2 = ThreadLocal.withInitial(
            () -> Pattern.compile("[.]+")
    );

    private static final int UNESCAPE_MAX_DEPTH = 1024;

    public List<byte[]> makeHashesBinary(final String url)  {
        return makeHashes(url, this::sha256);
    }

    private <T> List<T> makeHashes(final String inputUrl, final Function<String, T> sha256) {
        final MutableUrl url = canonicalize(inputUrl);
        final Set<String> expressions = createSuffixPrefixExpressions(url);
        final List<T> hashes = expressions.stream().map(sha256).collect(Collectors.toList());
        return hashes;
    }

    Set<String> createSuffixPrefixExpressions(final MutableUrl url) {
        final Set<String> prefixes = createPrefix(url.getHost());
        final Set<String> suffixes = createSuffix(url.getPath(), url.getParams());
        final Set<String> result = new HashSet<>(prefixes.size() * suffixes.size());
        for (String prefix : prefixes) {
            for (String suffix : suffixes) {
                result.add(prefix + suffix);
            }
        }
        return result;
    }

    Set<String> createPrefix(final String host) {
        final Set<String> result = new HashSet<>(5);
        result.add(host);
        boolean ipAddress = isIpAddress(host);
        if (ipAddress) return result;
        final List<String> split = Arrays.asList(host.split("\\."));
        final int size = split.size();
        final int start = size > 5 ? size - 5 : 0;
        for (int fromIndex = start; fromIndex < size - 1; fromIndex++) {
            final String component = String.join(".", split.subList(fromIndex, size));
            result.add(component);
        }
        return result;
    }

    Set<String> createSuffix(final String path, final String params) {
        final Set<String> result = new HashSet<>(6);
        result.add(path);
        result.add(path + params);
        final List<String> split = Arrays.asList(path.split("/"));
        final int size = Math.min(split.size(), 5);
        for (int toIndex = 1; toIndex < size; toIndex++) {
            final String pathPart = buildPath(split, toIndex) + '/';
            result.add(pathPart);
        }
        String pathPart = buildPath(split, size);
        final boolean notLastComponent = size < split.size();
        final boolean lastEndsWithSlash = size == split.size() && path.endsWith("/");
        if (notLastComponent || lastEndsWithSlash) {
            pathPart = pathPart + '/';
        }
        result.add(pathPart);
        return result;
    }

    private String buildPath(final List<String> split, final int to) {
        return String.join("/", split.subList(0, to));
    }

    boolean isIpAddress(final String host) {
        return InetAddressValidator.getInstance().isValid(host);
    }

    String hashPrefix4(final String input) {
        return hashPrefix(input, 4);
    }

    private String hashPrefix(final String input, final int size) {
        final String hash = sha256Hex(input);
        return hash.substring(0, size);
    }

    String sha256Hex(final String input) {
        return DigestUtils.sha256Hex(input);
    }

    private byte[] sha256(final String input) {
        return DigestUtils.sha256(input);
    }

    MutableUrl canonicalize(final String input) {
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

    private void makeValid(final MutableUrl url) {
        String host = url.getHost();
        host = LEADING_TRAILING_DOTS.get().matcher(host).replaceAll("");
        host = CONSECUTIVE_DOTS.get().matcher(host).replaceAll(".");
        url.setHost(host);
    }

    private void removePort(final MutableUrl url) {
        url.setPort("");
    }

    private void canonicalizeHostAndPath(final MutableUrl url) {
        final String host = canonicalizeHostName(url.getHost());
        url.setHost(host);
        final String path = canonicalizePath(url.getPath());
        url.setPath(path);
    }

    private void percentUnescape(final MutableUrl url) {
        final String path = recursiveUnescape(url.getPath());
        url.setPath(path);
        final String host = recursiveUnescape(url.getHost());
        url.setHost(host);
    }

    private String recursiveUnescape(String path) {
        int i = 0;
        while (i < UNESCAPE_MAX_DEPTH) {
            final String prev = path;
            path = unescape(prev);
            if (prev.equals(path)) return path;
            i++;
        }
        throw new IllegalArgumentException("Safebrowsing: unescaping is too recursive!");
    }

    private String unescape(final String s) {
        final int len = s.length();
        if (len < 3) return s;
        final int end = len - 2;
        final StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < len; i++) {
            final char c = s.charAt(i);
            if (c=='%' && i<end && isHex(s.charAt(i+1)) && isHex(s.charAt(i+2))) {
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

    private boolean isHex(final char c) {
        return '0' <= c && c <= '9'
                || 'a' <= c && c <= 'f'
                || 'A' <= c && c <= 'F';
    }

    void removeSpecialCharacters(final MutableUrl url) {
        final String input = url.getUrl();
        final String result = SPECIAL_CHARACTERS.get().matcher(input).replaceAll("");
        url.setUrl(result);
    }

    void percentEscape(final MutableUrl url) {
        final String host = escape(url.getHost());
        url.setHost(host);
        final String path = escape(url.getPath());
        url.setPath(path);
    }

    String escape(final String url) {
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

    String canonicalizePath(String path) {
        path = CANONICAL_PATH_1.get().matcher(path).replaceAll("/");
        path = CANONICAL_PATH_2.get().matcher(path).replaceAll("/");
        return path;
    }

    String canonicalizeHostName(String host) {
        host = CANONICAL_HOST_1.get().matcher(host).replaceAll("");
        host = CANONICAL_HOST_2.get().matcher(host).replaceAll(".");
        host = normalizeIpAddress(host);
        host = host.toLowerCase(); // lowercase the whole string
        return host;
    }

    private String normalizeIpAddress(final String host) {
        return parseIPAddress(host);
    }

    void removeFragment(final MutableUrl mutableUrl) {
        final String url = mutableUrl.getUrl();
        final int i = url.indexOf('#'); //todo refactor
        if (i < 0) return;
        mutableUrl.setUrl(url.substring(0, i));
    }

    void convertToPunyCode(final MutableUrl url) {
        final String host = url.getHost();
        final String punyCode = punyCode(host);
        if (punyCode.equals(host)) return;
        url.setHost(punyCode);
    }

    String punyCode(final String domain) {
        return IDN.toASCII(domain);
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
    String parseIPAddress(String host) {
        if (host.length() <= 15) {
            final Matcher matcher = TRAILING_SPACE_REGEXP.get().matcher(host);
            if (matcher.matches()) {
                host = host.trim();
            }
        }
        final Matcher matcher = POSSIBLE_IP_REGEXP.get().matcher(host);
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
    private String canonicalNum(final String s, final int n) {
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

    private Long parseLong(final String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
