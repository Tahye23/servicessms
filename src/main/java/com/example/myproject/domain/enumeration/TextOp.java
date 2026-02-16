package com.example.myproject.domain.enumeration;

public enum TextOp {
    CONTAINS,
    STARTS_WITH,
    ENDS_WITH,
    EXACT,
    NOT_CONTAINS,
    NOT_STARTS_WITH,
    NOT_ENDS_WITH,
    NOT_EXACT;

    public static TextOp fromNullable(String raw, TextOp def) {
        if (raw == null || raw.isBlank()) return def;
        try {
            return TextOp.valueOf(raw.trim().toUpperCase());
        } catch (Exception e) {
            return def;
        }
    }

    /** valeurs attendues côté SQL (pour CASE) */
    public String sqlKey() {
        return switch (this) {
            case CONTAINS -> "contains";
            case STARTS_WITH -> "starts_with";
            case ENDS_WITH -> "ends_with";
            case EXACT -> "exact";
            case NOT_CONTAINS -> "not_contains";
            case NOT_STARTS_WITH -> "not_starts_with";
            case NOT_ENDS_WITH -> "not_ends_with";
            case NOT_EXACT -> "not_exact";
        };
    }
}
