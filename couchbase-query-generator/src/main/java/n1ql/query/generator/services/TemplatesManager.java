package n1ql.query.generator.services;

import n1ql.query.generator.model.QueryTemplate;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing query templates with persistence.
 */
@Service(Service.Level.APP)
@State(
    name = "N1QLQueryTemplates",
    storages = @Storage("n1ql-query-templates.xml")
)
public final class TemplatesManager implements PersistentStateComponent<TemplatesManager.State> {

    private State myState = new State();
    private final List<Runnable> listeners = new ArrayList<>();
    private boolean builtInTemplatesLoaded = false;

    public static TemplatesManager getInstance() {
        return ApplicationManager.getApplication().getService(TemplatesManager.class);
    }

    public static class State {
        public List<TemplateState> templates = new ArrayList<>();
    }

    public static class TemplateState {
        public String id;
        public String name;
        public String description;
        public String category;
        public String query;
        public boolean builtIn;
        public int useCount;

        public TemplateState() {}

        public TemplateState(QueryTemplate template) {
            this.id = template.getId();
            this.name = template.getName();
            this.description = template.getDescription();
            this.category = template.getCategory();
            this.query = template.getQuery();
            this.builtIn = template.isBuiltIn();
            this.useCount = template.getUseCount();
        }

        public QueryTemplate toTemplate() {
            QueryTemplate template = new QueryTemplate();
            template.setId(id);
            template.setName(name);
            template.setDescription(description);
            template.setCategory(category);
            template.setQuery(query);
            template.setBuiltIn(builtIn);
            template.setUseCount(useCount);
            return template;
        }
    }

    @Override
    public @Nullable State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
        XmlSerializerUtil.copyBean(state, myState);
    }

    /**
     * Ensures built-in templates are loaded.
     */
    public void ensureBuiltInTemplates() {
        if (builtInTemplatesLoaded) {
            return;
        }

        // Check if built-in templates exist
        boolean hasBuiltIn = myState.templates.stream().anyMatch(t -> t.builtIn);
        if (!hasBuiltIn) {
            loadBuiltInTemplates();
        }
        builtInTemplatesLoaded = true;
    }

    private void loadBuiltInTemplates() {
        List<QueryTemplate> builtIns = createBuiltInTemplates();
        for (QueryTemplate template : builtIns) {
            myState.templates.add(new TemplateState(template));
        }
    }

    private List<QueryTemplate> createBuiltInTemplates() {
        List<QueryTemplate> templates = new ArrayList<>();

        // SELECT Templates
        templates.add(new QueryTemplate(
            "Basic SELECT",
            "SELECT *\nFROM `bucket`\nWHERE type = \"document_type\"\nLIMIT 100",
            "Simple SELECT with type filter",
            "SELECT",
            true
        ));

        templates.add(new QueryTemplate(
            "Pagination Query",
            "SELECT *\nFROM `bucket`\nWHERE type = \"document_type\"\nORDER BY created_at DESC\nLIMIT 20 OFFSET 0",
            "Paginated results with sorting",
            "SELECT",
            true
        ));

        templates.add(new QueryTemplate(
            "Search by Field",
            "SELECT *\nFROM `bucket`\nWHERE type = \"document_type\"\n  AND LOWER(name) LIKE \"%search_term%\"\nLIMIT 50",
            "Case-insensitive search",
            "SELECT",
            true
        ));

        templates.add(new QueryTemplate(
            "Count Documents",
            "SELECT COUNT(*) AS total\nFROM `bucket`\nWHERE type = \"document_type\"",
            "Count matching documents",
            "Aggregation",
            true
        ));

        templates.add(new QueryTemplate(
            "Group By with Count",
            "SELECT status, COUNT(*) AS count\nFROM `bucket`\nWHERE type = \"document_type\"\nGROUP BY status\nORDER BY count DESC",
            "Group documents by status with count",
            "Aggregation",
            true
        ));

        templates.add(new QueryTemplate(
            "Sum and Average",
            "SELECT \n  SUM(amount) AS total_amount,\n  AVG(amount) AS avg_amount,\n  COUNT(*) AS count\nFROM `bucket`\nWHERE type = \"order\"",
            "Calculate sum and average",
            "Aggregation",
            true
        ));

        templates.add(new QueryTemplate(
            "Date Range Query",
            "SELECT *\nFROM `bucket`\nWHERE type = \"document_type\"\n  AND created_at >= \"2024-01-01\"\n  AND created_at < \"2024-02-01\"\nORDER BY created_at",
            "Query by date range",
            "SELECT",
            true
        ));

        templates.add(new QueryTemplate(
            "Array Contains",
            "SELECT *\nFROM `bucket`\nWHERE type = \"document_type\"\n  AND ANY tag IN tags SATISFIES tag = \"important\" END",
            "Find documents where array contains value",
            "SELECT",
            true
        ));

        // INSERT Templates
        templates.add(new QueryTemplate(
            "Insert Document",
            "INSERT INTO `bucket` (KEY, VALUE)\nVALUES (\n  UUID(),\n  {\n    \"type\": \"document_type\",\n    \"name\": \"New Document\",\n    \"created_at\": NOW_STR()\n  }\n)\nRETURNING *",
            "Insert a new document with auto-generated key",
            "INSERT",
            true
        ));

        // UPDATE Templates
        templates.add(new QueryTemplate(
            "Update Single Field",
            "UPDATE `bucket`\nSET status = \"active\"\nWHERE META().id = \"document_id\"\nRETURNING *",
            "Update a single field by document ID",
            "UPDATE",
            true
        ));

        templates.add(new QueryTemplate(
            "Bulk Update",
            "UPDATE `bucket`\nSET status = \"archived\",\n    updated_at = NOW_STR()\nWHERE type = \"document_type\"\n  AND status = \"inactive\"\nRETURNING META().id",
            "Update multiple documents matching criteria",
            "UPDATE",
            true
        ));

        // DELETE Templates
        templates.add(new QueryTemplate(
            "Delete by ID",
            "DELETE FROM `bucket`\nWHERE META().id = \"document_id\"\nRETURNING *",
            "Delete a specific document by ID",
            "DELETE",
            true
        ));

        templates.add(new QueryTemplate(
            "Bulk Delete",
            "DELETE FROM `bucket`\nWHERE type = \"document_type\"\n  AND status = \"deleted\"\n  AND created_at < \"2023-01-01\"\nRETURNING META().id",
            "Delete multiple old documents",
            "DELETE",
            true
        ));

        // UPSERT Templates
        templates.add(new QueryTemplate(
            "Upsert Document",
            "UPSERT INTO `bucket` (KEY, VALUE)\nVALUES (\n  \"user::12345\",\n  {\n    \"type\": \"user\",\n    \"name\": \"John Doe\",\n    \"email\": \"john@example.com\",\n    \"updated_at\": NOW_STR()\n  }\n)\nRETURNING *",
            "Insert or update a document",
            "UPSERT",
            true
        ));

        return templates;
    }

    /**
     * Gets all templates.
     */
    public List<QueryTemplate> getAllTemplates() {
        ensureBuiltInTemplates();
        return myState.templates.stream()
            .map(TemplateState::toTemplate)
            .collect(Collectors.toList());
    }

    /**
     * Gets templates by category.
     */
    public List<QueryTemplate> getTemplatesByCategory(String category) {
        ensureBuiltInTemplates();
        return myState.templates.stream()
            .filter(t -> t.category != null && t.category.equals(category))
            .map(TemplateState::toTemplate)
            .collect(Collectors.toList());
    }

    /**
     * Gets all categories.
     */
    public List<String> getCategories() {
        ensureBuiltInTemplates();
        return myState.templates.stream()
            .map(t -> t.category)
            .filter(Objects::nonNull)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    /**
     * Adds a custom template.
     */
    public void addTemplate(String name, String query, String description, String category) {
        QueryTemplate template = new QueryTemplate(name, query, description, category, false);
        myState.templates.add(new TemplateState(template));
        notifyListeners();
    }

    /**
     * Updates a template.
     */
    public void updateTemplate(String id, String name, String query, String description, String category) {
        for (TemplateState state : myState.templates) {
            if (state.id != null && state.id.equals(id) && !state.builtIn) {
                state.name = name;
                state.query = query;
                state.description = description;
                state.category = category;
                notifyListeners();
                break;
            }
        }
    }

    /**
     * Deletes a custom template.
     */
    public void deleteTemplate(String id) {
        myState.templates.removeIf(t -> t.id != null && t.id.equals(id) && !t.builtIn);
        notifyListeners();
    }

    /**
     * Records template usage.
     */
    public void recordUsage(String id) {
        for (TemplateState state : myState.templates) {
            if (state.id != null && state.id.equals(id)) {
                state.useCount++;
                break;
            }
        }
    }

    /**
     * Adds a listener.
     */
    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    /**
     * Removes a listener.
     */
    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }
}
