package n1ql.query.generator.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a saved query template.
 */
public class QueryTemplate {
    private String id;
    private String name;
    private String description;
    private String category;
    private String query;
    private boolean builtIn;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
    private int useCount;

    public QueryTemplate() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.useCount = 0;
        this.builtIn = false;
    }

    public QueryTemplate(String name, String query, String description, String category, boolean builtIn) {
        this();
        this.name = name;
        this.query = query;
        this.description = description;
        this.category = category;
        this.builtIn = builtIn;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public boolean isBuiltIn() {
        return builtIn;
    }

    public void setBuiltIn(boolean builtIn) {
        this.builtIn = builtIn;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public int getUseCount() {
        return useCount;
    }

    public void setUseCount(int useCount) {
        this.useCount = useCount;
    }

    public void incrementUseCount() {
        this.useCount++;
        this.lastUsedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return name;
    }
}
