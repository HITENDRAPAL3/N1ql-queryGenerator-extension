package n1ql.query.generator.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Main model class representing a complete N1QL query configuration.
 */
public class QueryModel {
    // Common fields
    private QueryOperation operation;
    private String bucket;
    private String scope;
    private String collection;
    
    // SELECT specific
    private List<String> selectFields;
    private boolean selectAll;
    private boolean distinct;
    
    // WHERE clause
    private List<WhereCondition> whereConditions;
    
    // ORDER BY
    private List<OrderByClause> orderByClauses;
    
    // Aggregation (GROUP BY, HAVING)
    private List<AggregationClause> aggregations;
    private List<String> groupByFields;
    private List<WhereCondition> havingConditions;
    
    // LIMIT and OFFSET
    private Integer limit;
    private Integer offset;
    
    // INSERT/UPSERT specific
    private String documentKey;
    private String documentValue; // JSON string
    
    // UPDATE specific
    private List<SetClause> setClauses;
    
    // Returning clause
    private boolean returningAll;
    private List<String> returningFields;

    public QueryModel() {
        this.operation = QueryOperation.SELECT;
        this.selectFields = new ArrayList<>();
        this.whereConditions = new ArrayList<>();
        this.orderByClauses = new ArrayList<>();
        this.aggregations = new ArrayList<>();
        this.groupByFields = new ArrayList<>();
        this.havingConditions = new ArrayList<>();
        this.setClauses = new ArrayList<>();
        this.returningFields = new ArrayList<>();
        this.selectAll = true;
    }

    // Getters and Setters
    public QueryOperation getOperation() {
        return operation;
    }

    public void setOperation(QueryOperation operation) {
        this.operation = operation;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public List<String> getSelectFields() {
        return selectFields;
    }

    public void setSelectFields(List<String> selectFields) {
        this.selectFields = selectFields;
    }

    public boolean isSelectAll() {
        return selectAll;
    }

    public void setSelectAll(boolean selectAll) {
        this.selectAll = selectAll;
    }

    public boolean isDistinct() {
        return distinct;
    }

    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    public List<WhereCondition> getWhereConditions() {
        return whereConditions;
    }

    public void setWhereConditions(List<WhereCondition> whereConditions) {
        this.whereConditions = whereConditions;
    }

    public void addWhereCondition(WhereCondition condition) {
        this.whereConditions.add(condition);
    }

    public void removeWhereCondition(int index) {
        if (index >= 0 && index < whereConditions.size()) {
            whereConditions.remove(index);
        }
    }

    public List<OrderByClause> getOrderByClauses() {
        return orderByClauses;
    }

    public void setOrderByClauses(List<OrderByClause> orderByClauses) {
        this.orderByClauses = orderByClauses;
    }

    public void addOrderByClause(OrderByClause clause) {
        this.orderByClauses.add(clause);
    }

    public List<AggregationClause> getAggregations() {
        return aggregations;
    }

    public void setAggregations(List<AggregationClause> aggregations) {
        this.aggregations = aggregations;
    }

    public void addAggregation(AggregationClause aggregation) {
        this.aggregations.add(aggregation);
    }

    public List<String> getGroupByFields() {
        return groupByFields;
    }

    public void setGroupByFields(List<String> groupByFields) {
        this.groupByFields = groupByFields;
    }

    public List<WhereCondition> getHavingConditions() {
        return havingConditions;
    }

    public void setHavingConditions(List<WhereCondition> havingConditions) {
        this.havingConditions = havingConditions;
    }

    public boolean hasAggregation() {
        return !aggregations.isEmpty() || !groupByFields.isEmpty();
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public String getDocumentKey() {
        return documentKey;
    }

    public void setDocumentKey(String documentKey) {
        this.documentKey = documentKey;
    }

    public String getDocumentValue() {
        return documentValue;
    }

    public void setDocumentValue(String documentValue) {
        this.documentValue = documentValue;
    }

    public List<SetClause> getSetClauses() {
        return setClauses;
    }

    public void setSetClauses(List<SetClause> setClauses) {
        this.setClauses = setClauses;
    }

    public void addSetClause(SetClause clause) {
        this.setClauses.add(clause);
    }

    public boolean isReturningAll() {
        return returningAll;
    }

    public void setReturningAll(boolean returningAll) {
        this.returningAll = returningAll;
    }

    public List<String> getReturningFields() {
        return returningFields;
    }

    public void setReturningFields(List<String> returningFields) {
        this.returningFields = returningFields;
    }

    /**
     * Returns the fully qualified keyspace name (bucket.scope.collection or just bucket).
     */
    public String getKeyspace() {
        StringBuilder sb = new StringBuilder();
        sb.append("`").append(bucket != null ? bucket : "bucket").append("`");
        
        if (scope != null && !scope.trim().isEmpty()) {
            sb.append(".`").append(scope).append("`");
            if (collection != null && !collection.trim().isEmpty()) {
                sb.append(".`").append(collection).append("`");
            }
        } else if (collection != null && !collection.trim().isEmpty()) {
            sb.append(".`_default`.`").append(collection).append("`");
        }
        
        return sb.toString();
    }

    /**
     * Resets the model to default state.
     */
    public void reset() {
        this.operation = QueryOperation.SELECT;
        this.bucket = null;
        this.scope = null;
        this.collection = null;
        this.selectFields.clear();
        this.selectAll = true;
        this.distinct = false;
        this.whereConditions.clear();
        this.orderByClauses.clear();
        this.aggregations.clear();
        this.groupByFields.clear();
        this.havingConditions.clear();
        this.limit = null;
        this.offset = null;
        this.documentKey = null;
        this.documentValue = null;
        this.setClauses.clear();
        this.returningAll = false;
        this.returningFields.clear();
    }
}
