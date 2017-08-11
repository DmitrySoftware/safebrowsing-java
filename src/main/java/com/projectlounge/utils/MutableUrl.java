package com.projectlounge.utils;

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

    @NonNull private String host;
    @NonNull private String path;
    @NonNull private String url;
    private int pathStart;
    private int pathEnd;
    private int hostStart;
    private int hostEnd;
    private int portIdx;

    public MutableUrl(final String url) {
        update(url);
    }

    private void update(final String _url) {
        if (null==_url) return;
        url = fixUrl(_url);
        final int schemeStart = url.indexOf(SCHEME);
        hostStart = schemeStart + SCHEME.length();
        portIdx = url.indexOf(PORT, hostStart);
        pathStart = url.indexOf(PATH, hostStart);
        pathStart = pathStart < 0 ? url.length() : pathStart;
        hostEnd = portIdx >= 0 ? portIdx : pathStart;
        host = url.substring(hostStart, hostEnd);
        int fragment = Utils.findFragmentIndex(url, pathStart);
        fragment = fragment<0 || fragment<pathStart ? Integer.MAX_VALUE : fragment;
        int params = url.indexOf("?");
        params = params < 0 ? Integer.MAX_VALUE : params;
        final int min = Math.min(fragment, params);
        pathEnd = (min == Integer.MAX_VALUE) ? url.length() : min;
        path = url.substring(pathStart, pathEnd);
    }

    private String fixUrl(final String url) {
        String result = url.trim();
        if (!result.contains(SCHEME)) {
            result = "http://" + result;
        }
        return result;
    }

    public void setHost(final String host) {
        if (null==host || host.trim().isEmpty()) return;
        replace(host, hostStart, hostEnd);
    }

    public void setPath(String path) {
        if (null==path) return;
        if (!path.startsWith("/")) path = "/" + path;
        replace(path, pathStart, pathEnd);
    }

    private void replace(final String host, final int from, final int to) {
        final StringBuilder sb = new StringBuilder(url.length());
        sb.append(url.substring(0, from));
        sb.append(host);
        sb.append(url.substring(to, url.length()));
        update(sb.toString());
    }

    public void setUrl(final String url) {
        update(url);
    }

    public void fixPath() {
        final String path = getPath();
        if (!path.startsWith("/")) {
            setPath("/" + path);
        }
    }

    public void removePort() {
        if (portIdx<0 || portIdx<hostEnd || portIdx>pathStart) return;
        replace("", portIdx, pathStart);
    }
}
