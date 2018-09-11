package com.ampro.weebot.util;

/**
 * Enum values for UNICODE values (like emoji).
 *
 * @author Jonathan Augustine
 * @since 1.1.0
 */
public enum Unicode {

    HEAVY_CHECK("\u2714"),
    CROSS_MARK("\u274c"),
    WARNING_TRIANGLE("\u26A0"),
    UP_BTTON("\u23EB"),
    DOWN_BUTTON("\u23EC"),
    PAUSE_BUTTON("\u23F9");

    public final String val;
    Unicode(String uni) { val = uni; }
}
