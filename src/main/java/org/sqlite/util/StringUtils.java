package org.sqlite.util;

import java.util.List;

public class StringUtils {
    public static String join(List<String> list, String separator) {
        return String.join(separator, list);
    }
}
