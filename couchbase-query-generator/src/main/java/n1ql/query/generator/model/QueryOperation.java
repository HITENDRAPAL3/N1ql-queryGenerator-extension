package n1ql.query.generator.model;

/**
 * Enum representing the supported N1QL query operations.
 */
public enum QueryOperation {
    SELECT("SELECT"),
    INSERT("INSERT"),
    UPDATE("UPDATE"),
    DELETE("DELETE"),
    UPSERT("UPSERT");

    private final String keyword;

    QueryOperation(String keyword) {
        this.keyword = keyword;
    }

    public String getKeyword() {
        return keyword;
    }

    @Override
    public String toString() {
        return keyword;
    }
}
