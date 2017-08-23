package com.projectlounge.json.enums;

/**
 * Created by main on 24.08.17.
 */
public enum PlatformType {

    /** Unknown platform. */
    PLATFORM_TYPE_UNSPECIFIED,

    /** Threat posed to Windows. */
    WINDOWS,

    /** Threat posed to Linux. */
    LINUX,

    /** Threat posed to Android. */
    ANDROID,

    /** Threat posed to OS X. */
    OSX,

    /** Threat posed to iOS. */
    IOS,

    /** Threat posed to at least one of the defined platforms. */
    ANY_PLATFORM,

    /** Threat posed to all defined platforms. */
    ALL_PLATFORMS,

    /** Threat posed to Chrome. */
    CHROME,
    ;
}
