package n1ql.query.generator.builder;

import n1ql.query.generator.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main query builder that delegates to operation-specific builders.
 */
public class N1QLQueryBuilder {

    private final QueryModel model;
    private boolean formatOutput = true;

    public N1QLQueryBuilder(QueryModel model) {
        this.model = model;
    }

    public void setFormatOutput(boolean formatOutput) {
        this.formatOutput = formatOutput;
    }

    /**
     * Builds the N1QL query string based on the model configuration.
     */
    public String build() {
        if (model == null) {
            return "-- No query model provided";
        }

        return switch (model.getOperation()) {
            case SELECT -> buildSelectQuery();
            case INSERT -> buildInsertQuery();
            case UPDATE -> buildUpdateQuery();
            case DELETE -> buildDeleteQuery();
            case UPSERT -> buildUpsertQuery();
        };
    }

    private String buildSelectQuery() {
        StringBuilder sb = new StringBuilder();
        
        // SELECT clause
        sb.append("SELECT ");
        if (model.isDistinct()) {
            sb.append("DISTINCT ");
        }
        
        // Check if we have aggregations
        boolean hasAggregation = model.hasAggregation();
        List<AggregationClause> aggregations = model.getAggregations();
        List<String> groupByFields = model.getGroupByFields();
        
        if (hasAggregation && !aggregations.isEmpty()) {
            // Build aggregation select
            List<String> selectParts = new ArrayList<>();
            
            // Add GROUP BY fields first
            for (String field : groupByFields) {
                selectParts.add(escapeFieldName(field));
            }
            
            // Add aggregation functions
            for (AggregationClause agg : aggregations) {
                if (agg.isValid()) {
                    selectParts.add(agg.toSql());
                }
            }
            
            sb.append(String.join(", ", selectParts));
        } else if (model.isSelectAll() || model.getSelectFields().isEmpty()) {
            sb.append("*");
        } else {
            sb.append(model.getSelectFields().stream()
                .map(this::escapeFieldName)
                .collect(Collectors.joining(", ")));
        }
        
        // FROM clause
        appendNewLineOrSpace(sb);
        sb.append("FROM ").append(model.getKeyspace());
        
        // WHERE clause
        appendWhereClause(sb);
        
        // GROUP BY clause
        appendGroupByClause(sb);
        
        // HAVING clause
        appendHavingClause(sb);
        
        // ORDER BY clause
        appendOrderByClause(sb);
        
        // LIMIT and OFFSET
        appendLimitOffset(sb);
        
        return sb.toString();
    }

    private String buildInsertQuery() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("INSERT INTO ").append(model.getKeyspace());
        
        // Document key and value
        appendNewLineOrSpace(sb);
        sb.append("(KEY, VALUE)");
        appendNewLineOrSpace(sb);
        sb.append("VALUES (");
        
        String key = model.getDocumentKey();
        if (key != null && !key.isEmpty()) {
            sb.append("\"").append(escapeString(key)).append("\"");
        } else {
            sb.append("UUID()");
        }
        
        sb.append(", ");
        
        String value = model.getDocumentValue();
        if (value != null && !value.isEmpty()) {
            sb.append(value);
        } else {
            sb.append("{}");
        }
        
        sb.append(")");
        
        // RETURNING clause
        appendReturningClause(sb);
        
        return sb.toString();
    }

    private String buildUpdateQuery() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("UPDATE ").append(model.getKeyspace());
        
        // SET clause
        if (!model.getSetClauses().isEmpty()) {
            appendNewLineOrSpace(sb);
            sb.append("SET ");
            
            List<SetClause> validClauses = model.getSetClauses().stream()
                .filter(SetClause::isValid)
                .toList();
            
            for (int i = 0; i < validClauses.size(); i++) {
                SetClause clause = validClauses.get(i);
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(escapeFieldName(clause.getField())).append(" = ");
                if (clause.isExpression()) {
                    sb.append(clause.getValue());
                } else {
                    sb.append(formatValue(clause.getValue()));
                }
            }
        }
        
        // WHERE clause (important for UPDATE!)
        appendWhereClause(sb);
        
        // RETURNING clause
        appendReturningClause(sb);
        
        return sb.toString();
    }

    private String buildDeleteQuery() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("DELETE FROM ").append(model.getKeyspace());
        
        // WHERE clause (important for DELETE!)
        appendWhereClause(sb);
        
        // RETURNING clause
        appendReturningClause(sb);
        
        return sb.toString();
    }

    private String buildUpsertQuery() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("UPSERT INTO ").append(model.getKeyspace());
        
        // Document key and value
        appendNewLineOrSpace(sb);
        sb.append("(KEY, VALUE)");
        appendNewLineOrSpace(sb);
        sb.append("VALUES (");
        
        String key = model.getDocumentKey();
        if (key != null && !key.isEmpty()) {
            sb.append("\"").append(escapeString(key)).append("\"");
        } else {
            sb.append("UUID()");
        }
        
        sb.append(", ");
        
        String value = model.getDocumentValue();
        if (value != null && !value.isEmpty()) {
            sb.append(value);
        } else {
            sb.append("{}");
        }
        
        sb.append(")");
        
        // RETURNING clause
        appendReturningClause(sb);
        
        return sb.toString();
    }

    private void appendWhereClause(StringBuilder sb) {
        List<WhereCondition> validConditions = model.getWhereConditions().stream()
            .filter(WhereCondition::isValid)
            .toList();
        
        if (validConditions.isEmpty()) {
            return;
        }
        
        appendNewLineOrSpace(sb);
        sb.append("WHERE ");
        
        for (int i = 0; i < validConditions.size(); i++) {
            WhereCondition condition = validConditions.get(i);
            
            if (i > 0) {
                sb.append(" ").append(validConditions.get(i - 1).getLogicalOperator().getSql()).append(" ");
            }
            
            sb.append(buildCondition(condition));
        }
    }

    private String buildCondition(WhereCondition condition) {
        StringBuilder sb = new StringBuilder();
        String field = escapeFieldName(condition.getField());
        WhereOperator op = condition.getOperator();
        
        switch (op) {
            case IS_NULL, IS_NOT_NULL -> {
                sb.append(field).append(" ").append(op.getSql());
            }
            case BETWEEN -> {
                sb.append(field).append(" BETWEEN ")
                    .append(formatValue(condition.getValue()))
                    .append(" AND ")
                    .append(formatValue(condition.getSecondValue()));
            }
            case IN, NOT_IN -> {
                sb.append(field).append(" ").append(op.getSql()).append(" ");
                if (condition.isSubquery()) {
                    sb.append("(").append(condition.getSubquery()).append(")");
                } else {
                    sb.append(formatInList(condition.getValue()));
                }
            }
            case LIKE, NOT_LIKE -> {
                sb.append(field).append(" ").append(op.getSql()).append(" ")
                    .append("\"").append(escapeString(condition.getValue())).append("\"");
            }
            case ARRAY_CONTAINS -> {
                // ANY v IN field SATISFIES v = value END
                sb.append("ANY v IN ").append(field)
                    .append(" SATISFIES v = ").append(formatValue(condition.getValue()))
                    .append(" END");
            }
            case CONTAINS -> {
                sb.append("CONTAINS(").append(field).append(", ")
                    .append("\"").append(escapeString(condition.getValue())).append("\")");
            }
            default -> {
                sb.append(field).append(" ").append(op.getSql()).append(" ");
                if (condition.isSubquery()) {
                    sb.append("(").append(condition.getSubquery()).append(")");
                } else {
                    sb.append(formatValue(condition.getValue()));
                }
            }
        }
        
        return sb.toString();
    }

    private void appendOrderByClause(StringBuilder sb) {
        List<OrderByClause> validClauses = model.getOrderByClauses().stream()
            .filter(OrderByClause::isValid)
            .toList();
        
        if (validClauses.isEmpty()) {
            return;
        }
        
        appendNewLineOrSpace(sb);
        sb.append("ORDER BY ");
        
        sb.append(validClauses.stream()
            .map(c -> escapeFieldName(c.getField()) + " " + c.getSortOrder().getSql())
            .collect(Collectors.joining(", ")));
    }

    private void appendGroupByClause(StringBuilder sb) {
        List<String> groupByFields = model.getGroupByFields();
        
        if (groupByFields == null || groupByFields.isEmpty()) {
            return;
        }
        
        appendNewLineOrSpace(sb);
        sb.append("GROUP BY ");
        
        sb.append(groupByFields.stream()
            .map(this::escapeFieldName)
            .collect(Collectors.joining(", ")));
    }

    private void appendHavingClause(StringBuilder sb) {
        List<WhereCondition> havingConditions = model.getHavingConditions();
        
        if (havingConditions == null || havingConditions.isEmpty()) {
            return;
        }
        
        List<WhereCondition> validConditions = havingConditions.stream()
            .filter(WhereCondition::isValid)
            .toList();
        
        if (validConditions.isEmpty()) {
            return;
        }
        
        appendNewLineOrSpace(sb);
        sb.append("HAVING ");
        
        for (int i = 0; i < validConditions.size(); i++) {
            WhereCondition condition = validConditions.get(i);
            
            if (i > 0) {
                sb.append(" ").append(validConditions.get(i - 1).getLogicalOperator().getSql()).append(" ");
            }
            
            sb.append(buildCondition(condition));
        }
    }

    private void appendLimitOffset(StringBuilder sb) {
        if (model.getLimit() != null && model.getLimit() > 0) {
            appendNewLineOrSpace(sb);
            sb.append("LIMIT ").append(model.getLimit());
        }
        
        if (model.getOffset() != null && model.getOffset() > 0) {
            if (model.getLimit() == null) {
                appendNewLineOrSpace(sb);
            } else {
                sb.append(" ");
            }
            sb.append("OFFSET ").append(model.getOffset());
        }
    }

    private void appendReturningClause(StringBuilder sb) {
        if (model.isReturningAll()) {
            appendNewLineOrSpace(sb);
            sb.append("RETURNING *");
        } else if (!model.getReturningFields().isEmpty()) {
            appendNewLineOrSpace(sb);
            sb.append("RETURNING ");
            sb.append(model.getReturningFields().stream()
                .map(this::escapeFieldName)
                .collect(Collectors.joining(", ")));
        }
    }

    private void appendNewLineOrSpace(StringBuilder sb) {
        if (formatOutput) {
            sb.append("\n");
        } else {
            sb.append(" ");
        }
    }

    /**
     * Escapes a field name with backticks if necessary.
     */
    private String escapeFieldName(String field) {
        if (field == null) return "";
        field = field.trim();
        
        // Don't escape if it's already escaped, a wildcard, or contains dots (nested field)
        if (field.startsWith("`") || field.equals("*") || field.contains(".")) {
            return field;
        }
        
        // Don't escape if it's a function call
        if (field.contains("(") && field.contains(")")) {
            return field;
        }
        
        // Escape if contains special characters or is a reserved word
        if (needsEscaping(field)) {
            return "`" + field + "`";
        }
        
        return field;
    }

    private boolean needsEscaping(String field) {
        // Check for special characters
        if (!field.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            return true;
        }
        
        // Check for N1QL reserved words (simplified list)
        String[] reserved = {"select", "from", "where", "order", "by", "limit", "offset",
            "insert", "update", "delete", "set", "values", "key", "value", "type",
            "and", "or", "not", "in", "like", "between", "is", "null", "true", "false"};
        
        String lower = field.toLowerCase();
        for (String word : reserved) {
            if (word.equals(lower)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Formats a value for N1QL (adds quotes for strings, etc.).
     */
    private String formatValue(String value) {
        if (value == null) return "NULL";
        value = value.trim();
        
        // Check if it's a number
        if (value.matches("^-?\\d+(\\.\\d+)?$")) {
            return value;
        }
        
        // Check if it's a boolean
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return value.toLowerCase();
        }
        
        // Check if it's NULL
        if (value.equalsIgnoreCase("null")) {
            return "NULL";
        }
        
        // Check if it's already a JSON object or array
        if ((value.startsWith("{") && value.endsWith("}")) ||
            (value.startsWith("[") && value.endsWith("]"))) {
            return value;
        }
        
        // Check if it's a function call or expression
        if (value.contains("(") && value.contains(")")) {
            return value;
        }
        
        // Otherwise, treat as string
        return "\"" + escapeString(value) + "\"";
    }

    /**
     * Formats a comma-separated list for IN clause.
     */
    private String formatInList(String value) {
        if (value == null) return "[]";
        
        // If it's already an array, return as-is
        if (value.trim().startsWith("[") && value.trim().endsWith("]")) {
            return value.trim();
        }
        
        // Parse comma-separated values
        String[] parts = value.split(",");
        StringBuilder sb = new StringBuilder("[");
        
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(formatValue(parts[i].trim()));
        }
        
        sb.append("]");
        return sb.toString();
    }

    /**
     * Escapes special characters in a string.
     */
    private String escapeString(String value) {
        if (value == null) return "";
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
