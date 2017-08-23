package com.projectlounge.model;

import com.projectlounge.utils.Utils;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * Created by main on 11.08.17.
 */
@Getter @Setter
public class MutableUrl {

    private static final String SCHEME = "://";
    private static final String PATH = "/";
    private static final String PORT = ":";

    @NonNull private String scheme;
    @NonNull private String port;
    @NonNull private String host;
    @NonNull private String path;
    @NonNull private String params;
    @NonNull private String fragment;

    public MutableUrl(final String url) {
        init(url);
    }

    private void init(final String input) {
        if (null==input || input.trim().isEmpty()) throw new IllegalArgumentException("Empty URL!");
        setDefaults();
        parseUrl(input);
    }

    private void setDefaults() {
        scheme = "";
        port = "";
        host = "";
        path = "";
        params = "";
        fragment = "";
    }

    private void parseUrl(final String input) {
        String url = input.trim();
        if (!url.contains(SCHEME)) {
            url = "http://" + url;
        }
        final int schemeStart = url.indexOf(SCHEME);
        final int hostStart = schemeStart + SCHEME.length();
        setScheme(url.substring(0, hostStart));
        final int portIdx = url.indexOf(PORT, hostStart);
        int pathStart = url.indexOf(PATH, hostStart);
        pathStart = pathStart < 0 ? url.length() : pathStart;
        if (portIdx > 0) setPort(url.substring(portIdx, pathStart));
        final int hostEnd = portIdx >= 0 ? portIdx : pathStart;
        setHost(url.substring(hostStart, hostEnd));
        int fragment = Utils.findFragmentIndex(url, pathStart);
        if (fragment<0 || fragment<pathStart) {
            fragment = url.length();
        } else {
            setFragment(url.substring(fragment));
        }
        int params = url.indexOf("?");
        if (params < 0) {
            params = url.length();
        } else {
            setParams(url.substring(params, fragment));
        }
        final int pathEnd = Math.min(fragment, params);
        setPath(url.substring(pathStart, pathEnd));
    }

    public void setPath(String path) {
        if (null==path) return;
        if (!path.startsWith("/")) path = "/" + path;
        this.path = path;
    }

    public void setUrl(final String url) {
        init(url);
    }

    public String getUrl() {
        return scheme + host + port + path + params + fragment;
    }

    @Override
    public String toString() {
        return getUrl();
    }
}
