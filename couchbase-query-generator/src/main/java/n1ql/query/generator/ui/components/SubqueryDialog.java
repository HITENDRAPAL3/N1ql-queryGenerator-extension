package n1ql.query.generator.ui.components;

import n1ql.query.generator.builder.N1QLQueryBuilder;
import n1ql.query.generator.model.*;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Dialog for building subqueries to be used in WHERE clauses.
 */
public class SubqueryDialog extends DialogWrapper {
    
    private final QueryModel subqueryModel;
    private JBTextField bucketField;
    private JBTextField scopeField;
    private JBTextField collectionField;
    private JBCheckBox selectAllCheckbox;
    private JBTextField fieldsField;
    private JPanel whereConditionsPanel;
    private final List<SubqueryConditionRow> conditionRows;
    private JBTextArea previewArea;
    private String generatedQuery;

    public SubqueryDialog(Component parent) {
        super(parent, true);
        this.subqueryModel = new QueryModel();
        this.subqueryModel.setOperation(QueryOperation.SELECT);
        this.conditionRows = new ArrayList<>();
        
        setTitle("Build Subquery");
        setSize(500, 450);
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(JBUI.Borders.empty(10));
        
        // Keyspace section
        mainPanel.add(createKeyspacePanel());
        mainPanel.add(Box.createVerticalStrut(10));
        
        // Fields section
        mainPanel.add(createFieldsPanel());
        mainPanel.add(Box.createVerticalStrut(10));
        
        // WHERE section
        mainPanel.add(createWherePanel());
        mainPanel.add(Box.createVerticalStrut(10));
        
        // Preview section
        mainPanel.add(createPreviewPanel());
        
        return mainPanel;
    }

    private JPanel createKeyspacePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(createTitledBorder("Subquery Keyspace"));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        panel.add(new JBLabel("Bucket:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        bucketField = new JBTextField();
        bucketField.getDocument().addDocumentListener(createUpdateListener());
        panel.add(bucketField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        panel.add(new JBLabel("Scope:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        scopeField = new JBTextField();
        scopeField.getDocument().addDocumentListener(createUpdateListener());
        panel.add(scopeField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        panel.add(new JBLabel("Collection:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        collectionField = new JBTextField();
        collectionField.getDocument().addDocumentListener(createUpdateListener());
        panel.add(collectionField, gbc);
        
        return panel;
    }

    private JPanel createFieldsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(createTitledBorder("SELECT Fields"));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        selectAllCheckbox = new JBCheckBox("SELECT *", true);
        selectAllCheckbox.addActionListener(e -> {
            fieldsField.setEnabled(!selectAllCheckbox.isSelected());
            updatePreview();
        });
        panel.add(selectAllCheckbox, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 1;
        fieldsField = new JBTextField();
        fieldsField.setEnabled(false);
        fieldsField.setToolTipText("Comma-separated field names");
        fieldsField.getDocument().addDocumentListener(createUpdateListener());
        panel.add(fieldsField, gbc);
        
        return panel;
    }

    private JPanel createWherePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(createTitledBorder("WHERE Conditions"));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        panel.setPreferredSize(new Dimension(450, 120));
        
        whereConditionsPanel = new JPanel();
        whereConditionsPanel.setLayout(new BoxLayout(whereConditionsPanel, BoxLayout.Y_AXIS));
        
        JScrollPane scrollPane = new JScrollPane(whereConditionsPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        panel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("+ Add Condition");
        addButton.addActionListener(e -> addConditionRow());
        buttonPanel.add(addButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    private JPanel createPreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(createTitledBorder("Subquery Preview"));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        panel.setPreferredSize(new Dimension(450, 80));
        
        previewArea = new JBTextArea(3, 40);
        previewArea.setEditable(false);
        previewArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        
        JScrollPane scrollPane = new JScrollPane(previewArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }

    private void addConditionRow() {
        SubqueryConditionRow row = new SubqueryConditionRow();
        conditionRows.add(row);
        whereConditionsPanel.add(row);
        whereConditionsPanel.revalidate();
        whereConditionsPanel.repaint();
        updatePreview();
    }

    private void removeConditionRow(SubqueryConditionRow row) {
        conditionRows.remove(row);
        whereConditionsPanel.remove(row);
        whereConditionsPanel.revalidate();
        whereConditionsPanel.repaint();
        updatePreview();
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
            public void insertUpdate(DocumentEvent e) { updatePreview(); }
            @Override
            public void removeUpdate(DocumentEvent e) { updatePreview(); }
            @Override
            public void changedUpdate(DocumentEvent e) { updatePreview(); }
        };
    }

    private void updatePreview() {
        // Update model
        subqueryModel.setBucket(bucketField.getText().trim());
        subqueryModel.setScope(scopeField.getText().trim());
        subqueryModel.setCollection(collectionField.getText().trim());
        subqueryModel.setSelectAll(selectAllCheckbox.isSelected());
        
        if (!selectAllCheckbox.isSelected()) {
            String fields = fieldsField.getText().trim();
            if (!fields.isEmpty()) {
                subqueryModel.setSelectFields(Arrays.asList(fields.split("\\s*,\\s*")));
            }
        }
        
        // Get WHERE conditions
        List<WhereCondition> conditions = new ArrayList<>();
        for (SubqueryConditionRow row : conditionRows) {
            WhereCondition condition = row.getCondition();
            if (condition != null) {
                conditions.add(condition);
            }
        }
        subqueryModel.setWhereConditions(conditions);
        
        // Build query
        N1QLQueryBuilder builder = new N1QLQueryBuilder(subqueryModel);
        builder.setFormatOutput(false);
        generatedQuery = builder.build();
        
        previewArea.setText(generatedQuery);
    }

    public String getGeneratedQuery() {
        return generatedQuery;
    }

    /**
     * Simple condition row for subquery WHERE clause.
     */
    private class SubqueryConditionRow extends JPanel {
        private final JBTextField fieldField;
        private final ComboBox<WhereOperator> operatorCombo;
        private final JBTextField valueField;
        private final ComboBox<LogicalOperator> logicalCombo;

        public SubqueryConditionRow() {
            super(new FlowLayout(FlowLayout.LEFT, 3, 2));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
            
            logicalCombo = new ComboBox<>(LogicalOperator.values());
            logicalCombo.setPreferredSize(new Dimension(55, 22));
            logicalCombo.addActionListener(e -> updatePreview());
            if (conditionRows.isEmpty()) {
                logicalCombo.setVisible(false);
            }
            add(logicalCombo);
            
            fieldField = new JBTextField(8);
            fieldField.getDocument().addDocumentListener(createUpdateListener());
            add(fieldField);
            
            operatorCombo = new ComboBox<>(new WhereOperator[]{
                WhereOperator.EQUALS, WhereOperator.NOT_EQUALS,
                WhereOperator.GREATER_THAN, WhereOperator.LESS_THAN,
                WhereOperator.LIKE, WhereOperator.IN
            });
            operatorCombo.setPreferredSize(new Dimension(80, 22));
            operatorCombo.addActionListener(e -> updatePreview());
            add(operatorCombo);
            
            valueField = new JBTextField(10);
            valueField.getDocument().addDocumentListener(createUpdateListener());
            add(valueField);
            
            JButton removeButton = new JButton("✕");
            removeButton.setPreferredSize(new Dimension(25, 22));
            removeButton.addActionListener(e -> removeConditionRow(this));
            add(removeButton);
        }

        public WhereCondition getCondition() {
            String field = fieldField.getText().trim();
            if (field.isEmpty()) return null;
            
            WhereCondition condition = new WhereCondition();
            condition.setField(field);
            condition.setOperator((WhereOperator) operatorCombo.getSelectedItem());
            condition.setValue(valueField.getText().trim());
            condition.setLogicalOperator((LogicalOperator) logicalCombo.getSelectedItem());
            return condition;
        }
    }
}
