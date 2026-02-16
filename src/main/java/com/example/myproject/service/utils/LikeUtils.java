package com.example.myproject.service.utils;

public final class LikeUtils {

    private LikeUtils() {}

    public static String toLike(String value, String type) {
        if (value == null || value.isBlank()) return null;
        String v = value.trim();
        if (type == null || type.equalsIgnoreCase("contains")) return "%" + v + "%";
        if (type.equalsIgnoreCase("startsWith")) return v + "%";
        if (type.equalsIgnoreCase("endsWith")) return "%" + v;
        if (type.equalsIgnoreCase("exact")) return v; // pas de %
        return "%" + v + "%";
    }
}
