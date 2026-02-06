package n1ql.query.generator.model;

/**
 * Enum representing N1QL aggregate functions.
 */
public enum AggregateFunction {
    COUNT("COUNT", "Count of items"),
    COUNT_DISTINCT("COUNT(DISTINCT)", "Count of distinct items"),
    SUM("SUM", "Sum of values"),
    AVG("AVG", "Average of values"),
    MIN("MIN", "Minimum value"),
    MAX("MAX", "Maximum value"),
    ARRAY_AGG("ARRAY_AGG", "Aggregate into array"),
    ARRAY_AGG_DISTINCT("ARRAY_AGG(DISTINCT)", "Aggregate distinct values into array");

    private final String sql;
    private final String description;

    AggregateFunction(String sql, String description) {
        this.sql = sql;
        this.description = description;
    }

    public String getSql() {
        return sql;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return sql + " - " + description;
    }

    /**
     * Formats the function call with the given field.
     */
    public String format(String field) {
        if (this == COUNT && (field == null || field.isEmpty() || field.equals("*"))) {
            return "COUNT(*)";
        }
        
        if (this == COUNT_DISTINCT) {
            return "COUNT(DISTINCT " + field + ")";
        }
        
        if (this == ARRAY_AGG_DISTINCT) {
            return "ARRAY_AGG(DISTINCT " + field + ")";
        }
        
        return sql + "(" + (field != null && !field.isEmpty() ? field : "*") + ")";
    }
}
