package n1ql.query.generator.ui.components;

import n1ql.query.generator.model.QueryHistoryEntry;
import n1ql.query.generator.services.QueryHistoryManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

/**
 * Panel for displaying and managing query history.
 */
public class QueryHistoryPanel extends JBPanel<QueryHistoryPanel> {

    private final QueryHistoryManager historyManager;
    private final Consumer<String> onQuerySelected;
    private final JBTextField searchField;
    private final JPanel historyListPanel;
    private final JBCheckBox showFavoritesOnly;

    public QueryHistoryPanel(Consumer<String> onQuerySelected) {
        super(new BorderLayout());
        this.onQuerySelected = onQuerySelected;
        this.historyManager = QueryHistoryManager.getInstance();

        setBorder(createTitledBorder("Query History"));
        setPreferredSize(new Dimension(300, 400));

        // Search panel
        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        searchPanel.setBorder(JBUI.Borders.empty(5));
        
        searchField = new JBTextField();
        searchField.getEmptyText().setText("Search history...");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { refreshHistory(); }
            @Override
            public void removeUpdate(DocumentEvent e) { refreshHistory(); }
            @Override
            public void changedUpdate(DocumentEvent e) { refreshHistory(); }
        });
        searchPanel.add(searchField, BorderLayout.CENTER);
        
        add(searchPanel, BorderLayout.NORTH);

        // History list
        historyListPanel = new JPanel();
        historyListPanel.setLayout(new BoxLayout(historyListPanel, BoxLayout.Y_AXIS));
        
        JBScrollPane scrollPane = new JBScrollPane(historyListPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // Bottom panel with filters and actions
        JPanel bottomPanel = new JPanel(new BorderLayout());
        
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        showFavoritesOnly = new JBCheckBox("Favorites only");
        showFavoritesOnly.addActionListener(e -> refreshHistory());
        filterPanel.add(showFavoritesOnly);
        bottomPanel.add(filterPanel, BorderLayout.WEST);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 2));
        JButton clearButton = new JButton("Clear");
        clearButton.setToolTipText("Clear non-favorite history");
        clearButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(this,
                "Clear all non-favorite history entries?",
                "Confirm Clear",
                JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                historyManager.clearHistory();
                refreshHistory();
            }
        });
        actionsPanel.add(clearButton);
        bottomPanel.add(actionsPanel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        // Register for history updates
        historyManager.addListener(this::refreshHistory);

        // Initial load
        refreshHistory();
    }

    private TitledBorder createTitledBorder(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(JBColor.border()),
            title
        );
        border.setTitleColor(JBColor.foreground());
        return border;
    }

    public void refreshHistory() {
        historyListPanel.removeAll();

        String searchTerm = searchField.getText();
        List<QueryHistoryEntry> entries;
        
        if (showFavoritesOnly.isSelected()) {
            entries = historyManager.getFavorites();
        } else if (searchTerm != null && !searchTerm.isEmpty()) {
            entries = historyManager.search(searchTerm);
        } else {
            entries = historyManager.getHistory();
        }

        if (entries.isEmpty()) {
            JBLabel emptyLabel = new JBLabel("No history entries");
            emptyLabel.setForeground(JBColor.GRAY);
            emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
            emptyLabel.setBorder(JBUI.Borders.empty(20));
            historyListPanel.add(emptyLabel);
        } else {
            for (QueryHistoryEntry entry : entries) {
                historyListPanel.add(new HistoryEntryRow(entry));
                historyListPanel.add(Box.createVerticalStrut(2));
            }
        }

        historyListPanel.add(Box.createVerticalGlue());
        historyListPanel.revalidate();
        historyListPanel.repaint();
    }

    /**
     * Row representing a single history entry.
     */
    private class HistoryEntryRow extends JPanel {
        private final QueryHistoryEntry entry;
        private boolean isHovered = false;

        public HistoryEntryRow(QueryHistoryEntry entry) {
            super(new BorderLayout(5, 2));
            this.entry = entry;
            
            setBorder(JBUI.Borders.empty(5, 8));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            // Left side - query info
            JPanel infoPanel = new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.setOpaque(false);

            JBLabel previewLabel = new JBLabel(entry.getQueryPreview());
            previewLabel.setFont(previewLabel.getFont().deriveFont(Font.PLAIN, 11f));
            infoPanel.add(previewLabel);

            JBLabel metaLabel = new JBLabel(entry.toString());
            metaLabel.setFont(metaLabel.getFont().deriveFont(Font.ITALIC, 9f));
            metaLabel.setForeground(JBColor.GRAY);
            infoPanel.add(metaLabel);

            add(infoPanel, BorderLayout.CENTER);

            // Right side - favorite button
            JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
            actionsPanel.setOpaque(false);

            JButton favoriteButton = new JButton(entry.isFavorite() ? "★" : "☆");
            favoriteButton.setPreferredSize(new Dimension(30, 25));
            favoriteButton.setToolTipText(entry.isFavorite() ? "Remove from favorites" : "Add to favorites");
            favoriteButton.addActionListener(e -> {
                historyManager.toggleFavorite(entry.getId());
                refreshHistory();
            });
            actionsPanel.add(favoriteButton);

            JButton deleteButton = new JButton("✕");
            deleteButton.setPreferredSize(new Dimension(30, 25));
            deleteButton.setToolTipText("Remove from history");
            deleteButton.addActionListener(e -> {
                historyManager.removeEntry(entry.getId());
                refreshHistory();
            });
            actionsPanel.add(deleteButton);

            add(actionsPanel, BorderLayout.EAST);

            // Hover effect
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    setBackground(JBColor.background().brighter());
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    setBackground(null);
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && onQuerySelected != null) {
                        onQuerySelected.accept(entry.getQuery());
                    }
                }
            });

            // Tooltip with full query
            setToolTipText("<html><pre>" + escapeHtml(entry.getQuery()) + "</pre></html>");
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (isHovered) {
                g.setColor(JBColor.background().brighter());
                g.fillRect(0, 0, getWidth(), getHeight());
            }
            super.paintComponent(g);
        }

        private String escapeHtml(String text) {
            if (text == null) return "";
            return text.replace("&", "&amp;")
                      .replace("<", "&lt;")
                      .replace(">", "&gt;")
                      .replace("\n", "<br>");
        }
    }
}
