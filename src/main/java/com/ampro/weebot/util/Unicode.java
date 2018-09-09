package com.ampro.weebot.util;

/**
 * Enum values for UNICODE values (like emoji).
 *
 * @author Jonathan Augustine
 * @since 1.1.0
 */
public enum Unicode {

    HEAVY_CHECK("U+2714"),
    CROSS_MARK("U+274C");

    public final String val;
    Unicode(String uni) { val = uni; }
}
