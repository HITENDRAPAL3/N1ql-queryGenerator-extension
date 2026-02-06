package n1ql.query.generator.model;

/**
 * Represents an aggregation clause (SELECT with aggregate function).
 */
public class AggregationClause {
    private AggregateFunction function;
    private String field;
    private String alias;

    public AggregationClause() {
        this.function = AggregateFunction.COUNT;
    }

    public AggregationClause(AggregateFunction function, String field) {
        this.function = function;
        this.field = field;
    }

    public AggregationClause(AggregateFunction function, String field, String alias) {
        this.function = function;
        this.field = field;
        this.alias = alias;
    }

    public AggregateFunction getFunction() {
        return function;
    }

    public void setFunction(AggregateFunction function) {
        this.function = function;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public boolean isValid() {
        return function != null;
    }

    /**
     * Generates the SQL representation of this aggregation.
     */
    public String toSql() {
        if (!isValid()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(function.format(field));

        if (alias != null && !alias.trim().isEmpty()) {
            sb.append(" AS ").append(alias.trim());
        }

        return sb.toString();
    }
}
