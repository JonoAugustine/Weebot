package com.ampro.weebot.util;

import java.util.Collection;

/**
 * @author Jonathan Augustine
 * @since 1.0
 */
public class StringUtils {
    /**
     * Join a collection of Strings
     *
     * @param list The list of Strings to join
     * @param del The char to place between each joined string
     * @return The joined strings
     */
    public String join(Collection<String> list, char del) {
        StringBuilder sb = new StringBuilder();
        list.forEach( it -> sb.append(it).append(del) );
        return sb.toString();
    }
}
