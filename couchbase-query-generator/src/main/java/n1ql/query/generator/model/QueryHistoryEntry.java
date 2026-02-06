package n1ql.query.generator.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Represents an entry in the query history.
 */
public class QueryHistoryEntry {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String id;
    private String query;
    private QueryOperation operation;
    private String bucket;
    private LocalDateTime timestamp;
    private boolean favorite;

    public QueryHistoryEntry() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.favorite = false;
    }

    public QueryHistoryEntry(String query, QueryOperation operation, String bucket) {
        this();
        this.query = query;
        this.operation = operation;
        this.bucket = bucket;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public void toggleFavorite() {
        this.favorite = !this.favorite;
    }

    public String getFormattedTimestamp() {
        return timestamp != null ? timestamp.format(FORMATTER) : "";
    }

    /**
     * Returns a short preview of the query (first 50 characters).
     */
    public String getQueryPreview() {
        if (query == null || query.isEmpty()) {
            return "";
        }
        String singleLine = query.replace("\n", " ").replace("\r", "").trim();
        if (singleLine.length() <= 60) {
            return singleLine;
        }
        return singleLine.substring(0, 57) + "...";
    }

    @Override
    public String toString() {
        return String.format("[%s] %s - %s", 
            operation != null ? operation.getKeyword() : "?",
            bucket != null ? bucket : "?",
            getFormattedTimestamp());
    }
}
