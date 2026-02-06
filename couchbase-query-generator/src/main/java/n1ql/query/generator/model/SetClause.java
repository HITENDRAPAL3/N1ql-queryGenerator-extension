package n1ql.query.generator.model;

/**
 * Represents a SET clause for UPDATE operations.
 */
public class SetClause {
    private String field;
    private String value;
    private boolean isExpression; // true if value is an expression, false if literal

    public SetClause() {
        this.isExpression = false;
    }

    public SetClause(String field, String value) {
        this();
        this.field = field;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isExpression() {
        return isExpression;
    }

    public void setExpression(boolean expression) {
        isExpression = expression;
    }

    public boolean isValid() {
        return field != null && !field.trim().isEmpty() 
            && value != null && !value.trim().isEmpty();
    }
}
