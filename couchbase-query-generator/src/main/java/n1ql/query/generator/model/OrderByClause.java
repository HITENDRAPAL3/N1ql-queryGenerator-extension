package n1ql.query.generator.model;

/**
 * Represents an ORDER BY clause in a query.
 */
public class OrderByClause {
    private String field;
    private SortOrder sortOrder;

    public OrderByClause() {
        this.sortOrder = SortOrder.ASC;
    }

    public OrderByClause(String field, SortOrder sortOrder) {
        this.field = field;
        this.sortOrder = sortOrder;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isValid() {
        return field != null && !field.trim().isEmpty();
    }
}
