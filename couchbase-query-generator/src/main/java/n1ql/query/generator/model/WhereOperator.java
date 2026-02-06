package n1ql.query.generator.model;

/**
 * Enum representing the supported WHERE clause operators.
 */
public enum WhereOperator {
    EQUALS("=", "Equals", true),
    NOT_EQUALS("!=", "Not Equals", true),
    GREATER_THAN(">", "Greater Than", true),
    LESS_THAN("<", "Less Than", true),
    GREATER_THAN_OR_EQUALS(">=", "Greater Than or Equals", true),
    LESS_THAN_OR_EQUALS("<=", "Less Than or Equals", true),
    LIKE("LIKE", "Like (Pattern)", true),
    NOT_LIKE("NOT LIKE", "Not Like", true),
    IN("IN", "In (List)", true),
    NOT_IN("NOT IN", "Not In", true),
    IS_NULL("IS NULL", "Is Null", false),
    IS_NOT_NULL("IS NOT NULL", "Is Not Null", false),
    BETWEEN("BETWEEN", "Between", true),
    CONTAINS("CONTAINS", "Contains", true),
    ARRAY_CONTAINS("ANY ... IN ... SATISFIES", "Array Contains", true);

    private final String sql;
    private final String displayName;
    private final boolean requiresValue;

    WhereOperator(String sql, String displayName, boolean requiresValue) {
        this.sql = sql;
        this.displayName = displayName;
        this.requiresValue = requiresValue;
    }

    public String getSql() {
        return sql;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean requiresValue() {
        return requiresValue;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
