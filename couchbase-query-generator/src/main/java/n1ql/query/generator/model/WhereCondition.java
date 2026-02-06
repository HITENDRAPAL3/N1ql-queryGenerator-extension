package n1ql.query.generator.model;

/**
 * Represents a single WHERE condition in a query.
 */
public class WhereCondition {
    private String field;
    private WhereOperator operator;
    private String value;
    private String secondValue; // For BETWEEN operator
    private LogicalOperator logicalOperator; // How this condition connects to the next
    private boolean isSubquery;
    private String subquery;

    public WhereCondition() {
        this.operator = WhereOperator.EQUALS;
        this.logicalOperator = LogicalOperator.AND;
        this.isSubquery = false;
    }

    public WhereCondition(String field, WhereOperator operator, String value) {
        this();
        this.field = field;
        this.operator = operator;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public WhereOperator getOperator() {
        return operator;
    }

    public void setOperator(WhereOperator operator) {
        this.operator = operator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getSecondValue() {
        return secondValue;
    }

    public void setSecondValue(String secondValue) {
        this.secondValue = secondValue;
    }

    public LogicalOperator getLogicalOperator() {
        return logicalOperator;
    }

    public void setLogicalOperator(LogicalOperator logicalOperator) {
        this.logicalOperator = logicalOperator;
    }

    public boolean isSubquery() {
        return isSubquery;
    }

    public void setSubquery(boolean subquery) {
        isSubquery = subquery;
    }

    public String getSubquery() {
        return subquery;
    }

    public void setSubquery(String subquery) {
        this.subquery = subquery;
        this.isSubquery = subquery != null && !subquery.isEmpty();
    }

    public boolean isValid() {
        if (field == null || field.trim().isEmpty()) {
            return false;
        }
        if (!operator.requiresValue()) {
            return true;
        }
        if (isSubquery) {
            return subquery != null && !subquery.trim().isEmpty();
        }
        if (operator == WhereOperator.BETWEEN) {
            return value != null && !value.trim().isEmpty() 
                && secondValue != null && !secondValue.trim().isEmpty();
        }
        return value != null && !value.trim().isEmpty();
    }
}
