package n1ql.query.generator.services;

import n1ql.query.generator.model.QueryHistoryEntry;
import n1ql.query.generator.model.QueryOperation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing query history with persistence.
 */
@Service(Service.Level.APP)
@State(
    name = "N1QLQueryHistory",
    storages = @Storage("n1ql-query-history.xml")
)
public final class QueryHistoryManager implements PersistentStateComponent<QueryHistoryManager.State> {

    private static final int MAX_HISTORY_SIZE = 100;
    private State myState = new State();
    private final List<Runnable> listeners = new ArrayList<>();

    public static QueryHistoryManager getInstance() {
        return ApplicationManager.getApplication().getService(QueryHistoryManager.class);
    }

    public static class State {
        public List<HistoryEntryState> entries = new ArrayList<>();
    }

    public static class HistoryEntryState {
        public String id;
        public String query;
        public String operation;
        public String bucket;
        public String timestamp;
        public boolean favorite;

        public HistoryEntryState() {}

        public HistoryEntryState(QueryHistoryEntry entry) {
            this.id = entry.getId();
            this.query = entry.getQuery();
            this.operation = entry.getOperation() != null ? entry.getOperation().name() : null;
            this.bucket = entry.getBucket();
            this.timestamp = entry.getTimestamp() != null ? entry.getTimestamp().toString() : null;
            this.favorite = entry.isFavorite();
        }

        public QueryHistoryEntry toEntry() {
            QueryHistoryEntry entry = new QueryHistoryEntry();
            entry.setId(id);
            entry.setQuery(query);
            entry.setOperation(operation != null ? QueryOperation.valueOf(operation) : null);
            entry.setBucket(bucket);
            entry.setTimestamp(timestamp != null ? LocalDateTime.parse(timestamp) : LocalDateTime.now());
            entry.setFavorite(favorite);
            return entry;
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
     * Adds a new query to history.
     */
    public void addToHistory(String query, QueryOperation operation, String bucket) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }

        // Check for duplicate (same query in recent history)
        for (HistoryEntryState existing : myState.entries) {
            if (existing.query != null && existing.query.equals(query)) {
                // Move to top instead of adding duplicate
                myState.entries.remove(existing);
                existing.timestamp = LocalDateTime.now().toString();
                myState.entries.add(0, existing);
                notifyListeners();
                return;
            }
        }

        QueryHistoryEntry entry = new QueryHistoryEntry(query, operation, bucket);
        myState.entries.add(0, new HistoryEntryState(entry));

        // Trim to max size (keep favorites)
        trimHistory();
        notifyListeners();
    }

    private void trimHistory() {
        if (myState.entries.size() > MAX_HISTORY_SIZE) {
            // Keep favorites and most recent
            List<HistoryEntryState> favorites = myState.entries.stream()
                .filter(e -> e.favorite)
                .collect(Collectors.toList());
            
            List<HistoryEntryState> nonFavorites = myState.entries.stream()
                .filter(e -> !e.favorite)
                .limit(MAX_HISTORY_SIZE - favorites.size())
                .collect(Collectors.toList());

            myState.entries.clear();
            myState.entries.addAll(favorites);
            myState.entries.addAll(nonFavorites);
        }
    }

    /**
     * Gets all history entries.
     */
    public List<QueryHistoryEntry> getHistory() {
        return myState.entries.stream()
            .map(HistoryEntryState::toEntry)
            .collect(Collectors.toList());
    }

    /**
     * Gets favorite entries only.
     */
    public List<QueryHistoryEntry> getFavorites() {
        return myState.entries.stream()
            .filter(e -> e.favorite)
            .map(HistoryEntryState::toEntry)
            .collect(Collectors.toList());
    }

    /**
     * Searches history by query content.
     */
    public List<QueryHistoryEntry> search(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getHistory();
        }
        
        String lower = searchTerm.toLowerCase();
        return myState.entries.stream()
            .filter(e -> (e.query != null && e.query.toLowerCase().contains(lower)) ||
                        (e.bucket != null && e.bucket.toLowerCase().contains(lower)))
            .map(HistoryEntryState::toEntry)
            .collect(Collectors.toList());
    }

    /**
     * Toggles favorite status for an entry.
     */
    public void toggleFavorite(String id) {
        for (HistoryEntryState entry : myState.entries) {
            if (entry.id != null && entry.id.equals(id)) {
                entry.favorite = !entry.favorite;
                notifyListeners();
                break;
            }
        }
    }

    /**
     * Removes an entry from history.
     */
    public void removeEntry(String id) {
        myState.entries.removeIf(e -> e.id != null && e.id.equals(id));
        notifyListeners();
    }

    /**
     * Clears all non-favorite history.
     */
    public void clearHistory() {
        myState.entries.removeIf(e -> !e.favorite);
        notifyListeners();
    }

    /**
     * Clears all history including favorites.
     */
    public void clearAllHistory() {
        myState.entries.clear();
        notifyListeners();
    }

    /**
     * Adds a listener to be notified when history changes.
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
