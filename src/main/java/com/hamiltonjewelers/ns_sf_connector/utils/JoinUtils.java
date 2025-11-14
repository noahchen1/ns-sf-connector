package com.hamiltonjewelers.ns_sf_connector.utils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class JoinUtils {
    private JoinUtils() {}

    public static String joinCommaStrings(List<String> list) {
        if (list == null || list.isEmpty()) return "";

        return String.join(",", list);
    }

    public static String joinComma(List<?> list) {
        if (list == null || list.isEmpty()) return "";

        return list.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.joining(","));
    }
}
