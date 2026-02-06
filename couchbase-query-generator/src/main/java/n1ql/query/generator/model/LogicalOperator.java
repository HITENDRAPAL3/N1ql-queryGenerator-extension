package n1ql.query.generator.model;

/**
 * Enum representing logical operators for combining WHERE conditions.
 */
public enum LogicalOperator {
    AND("AND"),
    OR("OR");

    private final String sql;

    LogicalOperator(String sql) {
        this.sql = sql;
    }

    public String getSql() {
        return sql;
    }

    @Override
    public String toString() {
        return sql;
    }
}
