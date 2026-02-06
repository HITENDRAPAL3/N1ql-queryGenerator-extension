package n1ql.query.generator.model;

/**
 * Enum representing sort order for ORDER BY clause.
 */
public enum SortOrder {
    ASC("ASC", "Ascending"),
    DESC("DESC", "Descending");

    private final String sql;
    private final String displayName;

    SortOrder(String sql, String displayName) {
        this.sql = sql;
        this.displayName = displayName;
    }

    public String getSql() {
        return sql;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
