package n1ql.query.generator.ui;

import n1ql.query.generator.builder.N1QLQueryBuilder;
import n1ql.query.generator.model.*;
import n1ql.query.generator.services.QueryHistoryManager;
import n1ql.query.generator.ui.components.*;
import n1ql.query.generator.ui.highlighting.N1QLSyntaxHighlighter;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Main panel for the N1QL Query Generator tool window.
 */
public class QueryBuilderPanel {
    
    private final Project project;
    private final QueryModel model;
    private final JBPanel<?> mainPanel;
    
    // Keyspace inputs
    private JBTextField bucketField;
    private JBTextField scopeField;
    private JBTextField collectionField;
    
    // Field suggestions from JSON import
    private List<String> fieldSuggestions = new ArrayList<>();
    
    // Operation selector
    private ComboBox<QueryOperation> operationCombo;
    
    // Cards for different operations
    private JPanel operationCardsPanel;
    private CardLayout cardLayout;
    
    // SELECT components
    private JBCheckBox selectAllCheckbox;
    private JBCheckBox distinctCheckbox;
    private AutocompleteTextField fieldsField;
    private WhereClausePanel whereClausePanel;
    private AggregationPanel aggregationPanel;
    private OrderByPanel orderByPanel;
    private JBTextField limitField;
    private JBTextField offsetField;
    
    // INSERT/UPSERT components
    private JBTextField documentKeyField;
    private JBTextArea documentValueArea;
    
    // UPDATE components
    private SetClausePanel setClausePanel;
    private WhereClausePanel updateWherePanel;
    
    // DELETE components
    private WhereClausePanel deleteWherePanel;
    
    // RETURNING components
    private JBCheckBox returningAllCheckbox;
    private JBTextField returningFieldsField;
    
    // Query preview with syntax highlighting
    private JTextPane queryPreviewPane;
    private JPanel queryPreviewPanel;
    private N1QLSyntaxHighlighter syntaxHighlighter;
    
    // Format checkbox
    private JBCheckBox formatCheckbox;
    
    // Side panels
    private JTabbedPane sideTabPane;
    private QueryHistoryPanel historyPanel;
    private TemplatesPanel templatesPanel;
    private JSplitPane splitPane;
    private boolean sidePanelVisible = false; // Hidden by default
    
    // Template mode tracking
    private String loadedTemplateQuery = null;  // Store the loaded template
    private boolean isTemplateMode = false;      // Flag to indicate template is active
    
    // Manual edit tracking
    private boolean isManuallyEdited = false;    // Track if user edited query directly
    private boolean isUpdatingPreview = false;   // Prevent DocumentListener triggering during updates

    public QueryBuilderPanel(Project project) {
        this.project = project;
        this.model = new QueryModel();
        this.mainPanel = new JBPanel<>(new BorderLayout());
        this.syntaxHighlighter = new N1QLSyntaxHighlighter();
        
        initializeUI();
        updateQueryPreview();
    }

    private void initializeUI() {
        // Main split pane: Query builder on left, History/Templates on right
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(1.0); // Give all space to left by default
        splitPane.setOneTouchExpandable(true); // Add expand/collapse arrows
        splitPane.setContinuousLayout(true);
        
        // Left side - Query builder
        JBScrollPane builderScrollPane = new JBScrollPane(createContentPanel());
        builderScrollPane.setBorder(JBUI.Borders.empty());
        builderScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        splitPane.setLeftComponent(builderScrollPane);
        
        // Right side - History and Templates tabs
        sideTabPane = new JTabbedPane();
        
        historyPanel = new QueryHistoryPanel(this::loadQueryFromHistory);
        sideTabPane.addTab("History", historyPanel);
        
        templatesPanel = new TemplatesPanel(this::loadQueryFromTemplate);
        sideTabPane.addTab("Templates", templatesPanel);
        
        splitPane.setRightComponent(sideTabPane);
        
        mainPanel.add(splitPane, BorderLayout.CENTER);
        
        // Hide side panel by default
        SwingUtilities.invokeLater(() -> {
            int location = splitPane.getWidth() - splitPane.getDividerSize();
            splitPane.setDividerLocation(location);
            sideTabPane.setVisible(false);
        });
    }

    private JPanel createContentPanel() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(JBUI.Borders.empty(10));
        
        // Header
        contentPanel.add(createHeaderPanel());
        contentPanel.add(Box.createVerticalStrut(10));
        
        // Keyspace section with JSON import button
        contentPanel.add(createKeyspacePanel());
        contentPanel.add(Box.createVerticalStrut(10));
        
        // Operation selector
        contentPanel.add(createOperationSelectorPanel());
        contentPanel.add(Box.createVerticalStrut(10));
        
        // Operation-specific cards
        contentPanel.add(createOperationCardsPanel());
        contentPanel.add(Box.createVerticalStrut(10));
        
        // Query preview with syntax highlighting
        contentPanel.add(createQueryPreviewPanel());
        contentPanel.add(Box.createVerticalStrut(10));
        
        // Action buttons
        contentPanel.add(createActionButtonsPanel());
        
        // Add glue to push everything up
        contentPanel.add(Box.createVerticalGlue());
        
        return contentPanel;
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        
        JBLabel titleLabel = new JBLabel("N1QL Query Generator");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        panel.add(titleLabel, BorderLayout.WEST);
        
        // Add toggle button for History/Templates panel
        JButton togglePanelButton = new JButton("📚 Show History & Templates");
        togglePanelButton.setToolTipText("Show/Hide History and Templates panel");
        togglePanelButton.addActionListener(e -> {
            toggleSidePanel();
            // Update button text based on visibility
            togglePanelButton.setText(sidePanelVisible ? "✖ Hide History & Templates" : "📚 Show History & Templates");
        });
        panel.add(togglePanelButton, BorderLayout.EAST);
        
        return panel;
    }
    
    private void toggleSidePanel() {
        sidePanelVisible = !sidePanelVisible;
        
        if (sidePanelVisible) {
            // Show side panel
            sideTabPane.setVisible(true);
            SwingUtilities.invokeLater(() -> {
                int location = (int)(splitPane.getWidth() * 0.65); // 65% for main, 35% for side
                splitPane.setDividerLocation(location);
                splitPane.setResizeWeight(0.65);
            });
        } else {
            // Hide side panel
            SwingUtilities.invokeLater(() -> {
                int location = splitPane.getWidth() - splitPane.getDividerSize();
                splitPane.setDividerLocation(location);
                splitPane.setResizeWeight(1.0);
                sideTabPane.setVisible(false);
            });
        }
    }

    private JPanel createKeyspacePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(createTitledBorder("Keyspace"));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Bucket
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        panel.add(new JBLabel("Bucket *:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        bucketField = new JBTextField();
        bucketField.setToolTipText("Enter the bucket name (required)");
        bucketField.getDocument().addDocumentListener(createUpdateListener());
        panel.add(bucketField, gbc);
        
        // Scope
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        panel.add(new JBLabel("Scope:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        scopeField = new JBTextField();
        scopeField.setToolTipText("Enter the scope name (optional, leave empty for default)");
        scopeField.getDocument().addDocumentListener(createUpdateListener());
        panel.add(scopeField, gbc);
        
        // Collection
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        panel.add(new JBLabel("Collection:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        collectionField = new JBTextField();
        collectionField.setToolTipText("Enter the collection name (optional, leave empty for default)");
        collectionField.getDocument().addDocumentListener(createUpdateListener());
        panel.add(collectionField, gbc);
        
        // JSON Import button
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        JButton importJsonButton = new JButton("📥 Import JSON Schema");
        importJsonButton.setToolTipText("Import a sample JSON document to auto-detect field names");
        importJsonButton.addActionListener(e -> showJsonImportDialog());
        panel.add(importJsonButton, gbc);
        
        return panel;
    }

    private void showJsonImportDialog() {
        JsonImportDialog dialog = new JsonImportDialog(mainPanel);
        if (dialog.showAndGet() && dialog.hasValidFields()) {
            fieldSuggestions = dialog.getExtractedFields();
            
            // Update all components with new suggestions
            updateFieldSuggestions();
            
            JOptionPane.showMessageDialog(mainPanel,
                "Imported " + fieldSuggestions.size() + " field suggestions!\nThey are now available in all input fields.",
                "Import Successful",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    /**
     * Propagates field suggestions to all relevant UI components.
     */
    private void updateFieldSuggestions() {
        if (fieldSuggestions == null || fieldSuggestions.isEmpty()) {
            return;
        }
        
        // Update SELECT fields
        if (fieldsField != null) {
            fieldsField.setSuggestions(fieldSuggestions);
        }
        
        // Update WHERE clause panels
        if (whereClausePanel != null) {
            whereClausePanel.updateFieldSuggestions(fieldSuggestions);
        }
        if (updateWherePanel != null) {
            updateWherePanel.updateFieldSuggestions(fieldSuggestions);
        }
        if (deleteWherePanel != null) {
            deleteWherePanel.updateFieldSuggestions(fieldSuggestions);
        }
        
        // Update SET clause panel
        if (setClausePanel != null) {
            setClausePanel.updateFieldSuggestions(fieldSuggestions);
        }
        
        // Update ORDER BY panel
        if (orderByPanel != null) {
            orderByPanel.updateFieldSuggestions(fieldSuggestions);
        }
        
        // Update Aggregation panel
        if (aggregationPanel != null) {
            aggregationPanel.updateFieldSuggestions(fieldSuggestions);
        }
    }

    private JPanel createOperationSelectorPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBorder(createTitledBorder("Operation"));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        
        operationCombo = new ComboBox<>(QueryOperation.values());
        operationCombo.setSelectedItem(QueryOperation.SELECT);
        operationCombo.addActionListener(e -> {
            QueryOperation selected = (QueryOperation) operationCombo.getSelectedItem();
            if (selected != null) {
                exitTemplateMode(); // Exit template mode when operation changes
                model.setOperation(selected);
                cardLayout.show(operationCardsPanel, selected.name());
                updateQueryPreview();
            }
        });
        
        panel.add(new JBLabel("Operation Type:"));
        panel.add(operationCombo);
        
        return panel;
    }

    private JPanel createOperationCardsPanel() {
        cardLayout = new CardLayout();
        operationCardsPanel = new JPanel(cardLayout);
        operationCardsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 600));
        
        // Create cards for each operation
        operationCardsPanel.add(createSelectPanel(), QueryOperation.SELECT.name());
        operationCardsPanel.add(createInsertPanel(), QueryOperation.INSERT.name());
        operationCardsPanel.add(createUpdatePanel(), QueryOperation.UPDATE.name());
        operationCardsPanel.add(createDeletePanel(), QueryOperation.DELETE.name());
        operationCardsPanel.add(createUpsertPanel(), QueryOperation.UPSERT.name());
        
        return operationCardsPanel;
    }

    private JPanel createSelectPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        // Fields section
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setBorder(createTitledBorder("SELECT Fields"));
        fieldsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        selectAllCheckbox = new JBCheckBox("SELECT *", true);
        selectAllCheckbox.addActionListener(e -> {
            model.setSelectAll(selectAllCheckbox.isSelected());
            fieldsField.setEnabled(!selectAllCheckbox.isSelected());
            updateQueryPreview();
        });
        fieldsPanel.add(selectAllCheckbox, gbc);
        
        gbc.gridx = 1;
        distinctCheckbox = new JBCheckBox("DISTINCT");
        distinctCheckbox.addActionListener(e -> {
            model.setDistinct(distinctCheckbox.isSelected());
            updateQueryPreview();
        });
        fieldsPanel.add(distinctCheckbox, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2; gbc.weightx = 1;
        fieldsField = new AutocompleteTextField(20);
        fieldsField.setToolTipText("Enter comma-separated field names (e.g., name, email, age). Press ↓ for suggestions.");
        fieldsField.setEnabled(false);
        fieldsField.getDocument().addDocumentListener(createUpdateListener());
        fieldsPanel.add(fieldsField, gbc);
        
        panel.add(fieldsPanel);
        panel.add(Box.createVerticalStrut(10));
        
        // WHERE section
        whereClausePanel = new WhereClausePanel(() -> {
            exitTemplateMode();
            updateQueryPreview();
        });
        panel.add(whereClausePanel);
        panel.add(Box.createVerticalStrut(10));
        
        // Aggregation section (NEW)
        aggregationPanel = new AggregationPanel(() -> {
            exitTemplateMode();
            updateQueryPreview();
        });
        panel.add(aggregationPanel);
        panel.add(Box.createVerticalStrut(10));
        
        // ORDER BY section
        orderByPanel = new OrderByPanel(() -> {
            exitTemplateMode();
            updateQueryPreview();
        });
        panel.add(orderByPanel);
        panel.add(Box.createVerticalStrut(10));
        
        // LIMIT/OFFSET section
        JPanel limitPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        limitPanel.setBorder(createTitledBorder("Pagination"));
        limitPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        
        limitPanel.add(new JBLabel("LIMIT:"));
        limitField = new JBTextField(8);
        limitField.setToolTipText("Maximum number of results");
        limitField.getDocument().addDocumentListener(createUpdateListener());
        limitPanel.add(limitField);
        
        limitPanel.add(new JBLabel("OFFSET:"));
        offsetField = new JBTextField(8);
        offsetField.setToolTipText("Number of results to skip");
        offsetField.getDocument().addDocumentListener(createUpdateListener());
        limitPanel.add(offsetField);
        
        panel.add(limitPanel);
        
        return panel;
    }

    private JPanel createInsertPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        // Document key
        JPanel keyPanel = new JPanel(new GridBagLayout());
        keyPanel.setBorder(createTitledBorder("Document"));
        keyPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        keyPanel.add(new JBLabel("Document Key:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        documentKeyField = new JBTextField();
        documentKeyField.setToolTipText("Leave empty to use UUID()");
        documentKeyField.getDocument().addDocumentListener(createUpdateListener());
        keyPanel.add(documentKeyField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        keyPanel.add(new JBLabel("Document Value (JSON):"), gbc);
        
        gbc.gridy = 2; gbc.weighty = 1; gbc.fill = GridBagConstraints.BOTH;
        documentValueArea = new JBTextArea(6, 40);
        documentValueArea.setToolTipText("Enter JSON document value");
        documentValueArea.setText("{\n  \n}");
        documentValueArea.getDocument().addDocumentListener(createUpdateListener());
        JBScrollPane scrollPane = new JBScrollPane(documentValueArea);
        keyPanel.add(scrollPane, gbc);
        
        panel.add(keyPanel);
        panel.add(Box.createVerticalStrut(10));
        
        // RETURNING section
        panel.add(createReturningPanel());
        
        return panel;
    }

    private JPanel createUpdatePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        // SET clause
        setClausePanel = new SetClausePanel(() -> {
            exitTemplateMode();
            updateQueryPreview();
        });
        panel.add(setClausePanel);
        panel.add(Box.createVerticalStrut(10));
        
        // WHERE clause
        updateWherePanel = new WhereClausePanel(() -> {
            exitTemplateMode();
            updateQueryPreview();
        });
        updateWherePanel.setBorder(createTitledBorder("WHERE Conditions (Important!)"));
        panel.add(updateWherePanel);
        panel.add(Box.createVerticalStrut(10));
        
        // RETURNING section
        panel.add(createReturningPanel());
        
        return panel;
    }

    private JPanel createDeletePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        // Warning label
        JPanel warningPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        warningPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        JBLabel warningLabel = new JBLabel("⚠️ Always use WHERE clause to avoid deleting all documents!");
        warningLabel.setForeground(JBColor.ORANGE);
        warningPanel.add(warningLabel);
        panel.add(warningPanel);
        panel.add(Box.createVerticalStrut(10));
        
        // WHERE clause
        deleteWherePanel = new WhereClausePanel(() -> {
            exitTemplateMode();
            updateQueryPreview();
        });
        deleteWherePanel.setBorder(createTitledBorder("WHERE Conditions (Required!)"));
        panel.add(deleteWherePanel);
        panel.add(Box.createVerticalStrut(10));
        
        // RETURNING section
        panel.add(createReturningPanel());
        
        return panel;
    }

    private JPanel createUpsertPanel() {
        // UPSERT is similar to INSERT
        return createInsertPanel();
    }

    private JPanel createReturningPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBorder(createTitledBorder("RETURNING Clause"));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        
        returningAllCheckbox = new JBCheckBox("RETURNING *");
        returningAllCheckbox.addActionListener(e -> {
            model.setReturningAll(returningAllCheckbox.isSelected());
            returningFieldsField.setEnabled(!returningAllCheckbox.isSelected());
            updateQueryPreview();
        });
        panel.add(returningAllCheckbox);
        
        panel.add(new JBLabel("or fields:"));
        returningFieldsField = new JBTextField(20);
        returningFieldsField.setToolTipText("Comma-separated field names");
        returningFieldsField.getDocument().addDocumentListener(createUpdateListener());
        panel.add(returningFieldsField);
        
        return panel;
    }

    private JPanel createQueryPreviewPanel() {
        queryPreviewPanel = new JPanel(new BorderLayout());
        queryPreviewPanel.setBorder(createTitledBorder("Generated Query"));
        queryPreviewPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        // Remove fixed width, only set height
        queryPreviewPanel.setPreferredSize(new Dimension(0, 150));
        
        // Use JTextPane for syntax highlighting
        queryPreviewPane = N1QLSyntaxHighlighter.createHighlightedTextPane();
        // Explicitly ensure it's editable
        queryPreviewPane.setEditable(true);
        
        // Track manual edits to the query
        queryPreviewPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (!isUpdatingPreview) {
                    isManuallyEdited = true;
                    updateQueryPreviewTitle();
                }
            }
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                if (!isUpdatingPreview) {
                    isManuallyEdited = true;
                    updateQueryPreviewTitle();
                }
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                // Style changes don't count as manual edits
            }
        });
        
        JBScrollPane scrollPane = new JBScrollPane(queryPreviewPane);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        queryPreviewPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Format checkbox
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        formatCheckbox = new JBCheckBox("Format Query", true);
        formatCheckbox.addActionListener(e -> updateQueryPreview());
        optionsPanel.add(formatCheckbox);
        
        JBCheckBox highlightCheckbox = new JBCheckBox("Syntax Highlighting", true);
        highlightCheckbox.addActionListener(e -> updateQueryPreview());
        optionsPanel.add(highlightCheckbox);
        
        queryPreviewPanel.add(optionsPanel, BorderLayout.SOUTH);
        
        return queryPreviewPanel;
    }

    private JPanel createActionButtonsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        
        JButton copyButton = new JButton("📋 Copy to Clipboard");
        copyButton.setToolTipText("Copy the generated query to clipboard");
        copyButton.addActionListener(e -> copyToClipboard());
        panel.add(copyButton);
        
        JButton insertButton = new JButton("📝 Insert at Cursor");
        insertButton.setToolTipText("Insert the query at current cursor position in editor");
        insertButton.addActionListener(e -> insertAtCursor());
        panel.add(insertButton);
        
        JButton saveTemplateButton = new JButton("💾 Save as Template");
        saveTemplateButton.setToolTipText("Save the current query as a template");
        saveTemplateButton.addActionListener(e -> saveAsTemplate());
        panel.add(saveTemplateButton);
        
        JButton resetButton = new JButton("🔄 Reset");
        resetButton.setToolTipText("Reset all fields to default values");
        resetButton.addActionListener(e -> resetForm());
        panel.add(resetButton);
        
        return panel;
    }

    private TitledBorder createTitledBorder(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(JBColor.border()),
            title
        );
        border.setTitleColor(JBColor.foreground());
        return border;
    }

    private DocumentListener createUpdateListener() {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { updateQueryPreview(); }
            @Override
            public void removeUpdate(DocumentEvent e) { updateQueryPreview(); }
            @Override
            public void changedUpdate(DocumentEvent e) { updateQueryPreview(); }
        };
    }

    private void updateQueryPreviewTitle() {
        if (queryPreviewPanel != null) {
            String title = isManuallyEdited ? "Generated Query (Manually Edited)" : "Generated Query";
            queryPreviewPanel.setBorder(createTitledBorder(title));
        }
    }
    
    private void updateQueryPreview() {
        if (isManuallyEdited) {
            // Don't overwrite manual edits
            return;
        }
        
        isUpdatingPreview = true;
        try {
            if (isTemplateMode && loadedTemplateQuery != null) {
            // Template mode: replace bucket placeholder with full keyspace
            String bucket = bucketField.getText().trim();
            String scope = scopeField.getText().trim();
            String collection = collectionField.getText().trim();
            
            // Build the full keyspace
            StringBuilder keyspace = new StringBuilder();
            if (!bucket.isEmpty()) {
                keyspace.append("`").append(bucket).append("`");
                
                if (!scope.isEmpty()) {
                    keyspace.append(".`").append(scope).append("`");
                    if (!collection.isEmpty()) {
                        keyspace.append(".`").append(collection).append("`");
                    }
                } else if (!collection.isEmpty()) {
                    // Collection without scope uses _default scope
                    keyspace.append(".`_default`.`").append(collection).append("`");
                }
            } else {
                keyspace.append("`bucket`"); // Keep placeholder if no bucket entered
            }
            
            String query = loadedTemplateQuery.replaceAll("`bucket`", keyspace.toString());
            
                queryPreviewPane.setText(query);
                syntaxHighlighter.highlight(queryPreviewPane);
                queryPreviewPane.setCaretPosition(0);
            } else {
                // Normal mode: build from model
                updateModelFromUI();
                
                N1QLQueryBuilder builder = new N1QLQueryBuilder(model);
                builder.setFormatOutput(formatCheckbox.isSelected());
                String query = builder.build();
                
                // Update preview with syntax highlighting
                queryPreviewPane.setText(query);
                syntaxHighlighter.highlight(queryPreviewPane);
                queryPreviewPane.setCaretPosition(0);
            }
        } finally {
            isUpdatingPreview = false;
        }
    }

    private void updateModelFromUI() {
        // Keyspace
        model.setBucket(bucketField.getText().trim());
        model.setScope(scopeField.getText().trim());
        model.setCollection(collectionField.getText().trim());
        
        // Operation-specific updates
        QueryOperation operation = model.getOperation();
        
        switch (operation) {
            case SELECT -> {
                model.setSelectAll(selectAllCheckbox.isSelected());
                model.setDistinct(distinctCheckbox.isSelected());
                
                if (!selectAllCheckbox.isSelected()) {
                    String fields = fieldsField.getText().trim();
                    if (!fields.isEmpty()) {
                        model.setSelectFields(Arrays.asList(fields.split("\\s*,\\s*")));
                    } else {
                        model.getSelectFields().clear();
                    }
                }
                
                // WHERE conditions
                model.setWhereConditions(whereClausePanel.getConditions());
                
                // Aggregation
                model.setAggregations(aggregationPanel.getAggregations());
                model.setGroupByFields(aggregationPanel.getGroupByFields());
                model.setHavingConditions(aggregationPanel.getHavingConditions());
                
                // ORDER BY
                model.setOrderByClauses(orderByPanel.getOrderByClauses());
                
                // LIMIT/OFFSET
                try {
                    String limitText = limitField.getText().trim();
                    model.setLimit(limitText.isEmpty() ? null : Integer.parseInt(limitText));
                } catch (NumberFormatException e) {
                    model.setLimit(null);
                }
                
                try {
                    String offsetText = offsetField.getText().trim();
                    model.setOffset(offsetText.isEmpty() ? null : Integer.parseInt(offsetText));
                } catch (NumberFormatException e) {
                    model.setOffset(null);
                }
            }
            case INSERT, UPSERT -> {
                model.setDocumentKey(documentKeyField.getText().trim());
                model.setDocumentValue(documentValueArea.getText().trim());
                updateReturningFromUI();
            }
            case UPDATE -> {
                model.setSetClauses(setClausePanel.getSetClauses());
                model.setWhereConditions(updateWherePanel.getConditions());
                updateReturningFromUI();
            }
            case DELETE -> {
                model.setWhereConditions(deleteWherePanel.getConditions());
                updateReturningFromUI();
            }
        }
    }

    private void updateReturningFromUI() {
        model.setReturningAll(returningAllCheckbox.isSelected());
        if (!returningAllCheckbox.isSelected()) {
            String fields = returningFieldsField.getText().trim();
            if (!fields.isEmpty()) {
                model.setReturningFields(Arrays.asList(fields.split("\\s*,\\s*")));
            } else {
                model.getReturningFields().clear();
            }
        }
    }

    private void copyToClipboard() {
        String query = queryPreviewPane.getText();
        StringSelection selection = new StringSelection(query);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, null);
        
        // Add to history
        QueryHistoryManager.getInstance().addToHistory(query, model.getOperation(), model.getBucket());
        
        // Show feedback
        JOptionPane.showMessageDialog(mainPanel, 
            "Query copied to clipboard!", 
            "Success", 
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void insertAtCursor() {
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) {
            JOptionPane.showMessageDialog(mainPanel,
                "No active editor found. Please open a file first.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String query = queryPreviewPane.getText();
        
        // Add to history
        QueryHistoryManager.getInstance().addToHistory(query, model.getOperation(), model.getBucket());
        
        WriteCommandAction.runWriteCommandAction(project, () -> {
            int offset = editor.getCaretModel().getOffset();
            editor.getDocument().insertString(offset, query);
        });
    }

    private void saveAsTemplate() {
        String query = queryPreviewPane.getText();
        if (query == null || query.trim().isEmpty()) {
            JOptionPane.showMessageDialog(mainPanel,
                "No query to save.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        String name = JOptionPane.showInputDialog(mainPanel,
            "Enter template name:",
            "Save as Template",
            JOptionPane.PLAIN_MESSAGE);

        if (name != null && !name.trim().isEmpty()) {
            n1ql.query.generator.services.TemplatesManager.getInstance().addTemplate(
                name.trim(),
                query,
                "",
                "Custom"
            );
            templatesPanel.refreshTemplates();
            JOptionPane.showMessageDialog(mainPanel,
                "Template saved successfully!",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void loadQueryFromHistory(String query) {
        // Parse and load query into the form (simplified - just show in preview)
        queryPreviewPane.setText(query);
        syntaxHighlighter.highlight(queryPreviewPane);
        JOptionPane.showMessageDialog(mainPanel,
            "Query loaded from history. You can copy or insert it.",
            "Query Loaded",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void loadQueryFromTemplate(String query) {
        // Store template and enable template mode
        loadedTemplateQuery = query;
        isTemplateMode = true;
        
        // Apply current keyspace if already entered
        String bucket = bucketField.getText().trim();
        String scope = scopeField.getText().trim();
        String collection = collectionField.getText().trim();
        
        String displayQuery = query;
        if (!bucket.isEmpty()) {
            // Build the full keyspace
            StringBuilder keyspace = new StringBuilder();
            keyspace.append("`").append(bucket).append("`");
            
            if (!scope.isEmpty()) {
                keyspace.append(".`").append(scope).append("`");
                if (!collection.isEmpty()) {
                    keyspace.append(".`").append(collection).append("`");
                }
            } else if (!collection.isEmpty()) {
                keyspace.append(".`_default`.`").append(collection).append("`");
            }
            
            displayQuery = query.replaceAll("`bucket`", keyspace.toString());
        }
        
        queryPreviewPane.setText(displayQuery);
        syntaxHighlighter.highlight(queryPreviewPane);
        
        JOptionPane.showMessageDialog(mainPanel,
            "Template loaded. Enter/update bucket/scope/collection to customize.",
            "Template Loaded",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void exitTemplateMode() {
        isTemplateMode = false;
        loadedTemplateQuery = null;
    }

    private void resetForm() {
        model.reset();
        exitTemplateMode();  // Exit template mode on reset
        
        // Reset UI
        bucketField.setText("");
        scopeField.setText("");
        collectionField.setText("");
        operationCombo.setSelectedItem(QueryOperation.SELECT);
        selectAllCheckbox.setSelected(true);
        distinctCheckbox.setSelected(false);
        fieldsField.setText("");
        fieldsField.setEnabled(false);
        whereClausePanel.reset();
        aggregationPanel.reset();
        orderByPanel.reset();
        limitField.setText("");
        offsetField.setText("");
        documentKeyField.setText("");
        documentValueArea.setText("{\n  \n}");
        setClausePanel.reset();
        updateWherePanel.reset();
        deleteWherePanel.reset();
        returningAllCheckbox.setSelected(false);
        returningFieldsField.setText("");
        
        // Clear manual edit flag to allow regeneration
        isManuallyEdited = false;
        updateQueryPreviewTitle();
        updateQueryPreview();
    }

    public JComponent getContent() {
        return mainPanel;
    }
}
