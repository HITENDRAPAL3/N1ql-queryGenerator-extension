package n1ql.query.generator.ui.components;

import n1ql.query.generator.model.LogicalOperator;
import n1ql.query.generator.model.WhereCondition;
import n1ql.query.generator.model.WhereOperator;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel for building WHERE clause conditions dynamically.
 */
public class WhereClausePanel extends JBPanel<WhereClausePanel> {
    
    private final List<ConditionRow> conditionRows;
    private final JPanel conditionsContainer;
    private final Runnable onChangeCallback;
    private List<String> fieldSuggestions = new ArrayList<>();

    public WhereClausePanel(Runnable onChangeCallback) {
        super(new BorderLayout());
        this.onChangeCallback = onChangeCallback;
        this.conditionRows = new ArrayList<>();
        
        setBorder(createTitledBorder("WHERE Conditions"));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        
        // Container for condition rows
        conditionsContainer = new JPanel();
        conditionsContainer.setLayout(new BoxLayout(conditionsContainer, BoxLayout.Y_AXIS));
        
        JScrollPane scrollPane = new JScrollPane(conditionsContainer);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setPreferredSize(new Dimension(0, 75)); // Width 0 = auto-adjust
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
        
        // Add button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("+ Add Condition");
        addButton.addActionListener(e -> addConditionRow());
        buttonPanel.add(addButton);
        
        JButton addSubqueryButton = new JButton("+ Add Subquery");
        addSubqueryButton.addActionListener(e -> addSubqueryRow());
        buttonPanel.add(addSubqueryButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private TitledBorder createTitledBorder(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(JBColor.border()),
            title
        );
        border.setTitleColor(JBColor.foreground());
        return border;
    }

    public void addConditionRow() {
        ConditionRow row = new ConditionRow(false);
        conditionRows.add(row);
        conditionsContainer.add(row);
        conditionsContainer.revalidate();
        conditionsContainer.repaint();
        notifyChange();
    }

    public void addSubqueryRow() {
        ConditionRow row = new ConditionRow(true);
        conditionRows.add(row);
        conditionsContainer.add(row);
        conditionsContainer.revalidate();
        conditionsContainer.repaint();
        notifyChange();
    }

    public void removeConditionRow(ConditionRow row) {
        conditionRows.remove(row);
        conditionsContainer.remove(row);
        conditionsContainer.revalidate();
        conditionsContainer.repaint();
        notifyChange();
    }

    public List<WhereCondition> getConditions() {
        List<WhereCondition> conditions = new ArrayList<>();
        for (ConditionRow row : conditionRows) {
            WhereCondition condition = row.getCondition();
            if (condition != null) {
                conditions.add(condition);
            }
        }
        return conditions;
    }

    public void reset() {
        conditionRows.clear();
        conditionsContainer.removeAll();
        conditionsContainer.revalidate();
        conditionsContainer.repaint();
        notifyChange();
    }
    
    /**
     * Updates field suggestions for all condition rows.
     */
    public void updateFieldSuggestions(List<String> suggestions) {
        this.fieldSuggestions = suggestions != null ? new ArrayList<>(suggestions) : new ArrayList<>();
        // Update existing rows
        for (ConditionRow row : conditionRows) {
            row.updateFieldSuggestions(this.fieldSuggestions);
        }
    }

    private void notifyChange() {
        if (onChangeCallback != null) {
            onChangeCallback.run();
        }
    }

    /**
     * Inner class representing a single condition row.
     */
    private class ConditionRow extends JPanel {
        private final AutocompleteTextField fieldField;
        private final ComboBox<WhereOperator> operatorCombo;
        private final JBTextField valueField;
        private final JBTextField secondValueField; // For BETWEEN
        private final JBTextField subqueryField;
        private final ComboBox<LogicalOperator> logicalCombo;
        private final JPanel valuePanel;
        private final CardLayout valueCardLayout;
        private final boolean isSubquery;

        public ConditionRow(boolean isSubquery) {
            super(new FlowLayout(FlowLayout.LEFT, 5, 2));
            this.isSubquery = isSubquery;
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            
            // Logical operator (AND/OR) - shown for all except first
            logicalCombo = new ComboBox<>(LogicalOperator.values());
            logicalCombo.setPreferredSize(new Dimension(60, 25));
            logicalCombo.addActionListener(e -> notifyChange());
            if (conditionRows.isEmpty()) {
                logicalCombo.setVisible(false);
            }
            add(logicalCombo);
            
            // Field name with autocomplete
            fieldField = new AutocompleteTextField(10);
            fieldField.setToolTipText("Field name (press ↓ for suggestions)");
            fieldField.setSuggestions(fieldSuggestions);
            fieldField.getDocument().addDocumentListener(createDocListener());
            add(fieldField);
            
            // Operator
            operatorCombo = new ComboBox<>(WhereOperator.values());
            operatorCombo.setPreferredSize(new Dimension(140, 25));
            operatorCombo.addActionListener(e -> {
                updateValuePanel();
                notifyChange();
            });
            add(operatorCombo);
            
            // Value panel with card layout
            valueCardLayout = new CardLayout();
            valuePanel = new JPanel(valueCardLayout);
            
            // Regular value field
            valueField = new JBTextField(12);
            valueField.setToolTipText("Value");
            valueField.getDocument().addDocumentListener(createDocListener());
            
            // Second value field for BETWEEN
            secondValueField = new JBTextField(8);
            secondValueField.setToolTipText("Second value for BETWEEN");
            secondValueField.getDocument().addDocumentListener(createDocListener());
            
            // Subquery field
            subqueryField = new JBTextField(20);
            subqueryField.setToolTipText("Enter subquery (SELECT ...)");
            subqueryField.getDocument().addDocumentListener(createDocListener());
            
            // Create value panels
            JPanel simpleValuePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            simpleValuePanel.add(valueField);
            valuePanel.add(simpleValuePanel, "SIMPLE");
            
            JPanel betweenPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
            JBTextField betweenValue1 = new JBTextField(8);
            betweenValue1.setToolTipText("Start value");
            betweenValue1.getDocument().addDocumentListener(createDocListener());
            betweenPanel.add(betweenValue1);
            betweenPanel.add(new JBLabel(" AND "));
            betweenPanel.add(secondValueField);
            valuePanel.add(betweenPanel, "BETWEEN");
            
            JPanel nullPanel = new JPanel();
            valuePanel.add(nullPanel, "NULL");
            
            JPanel subqueryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            subqueryPanel.add(new JBLabel("("));
            subqueryPanel.add(subqueryField);
            subqueryPanel.add(new JBLabel(")"));
            valuePanel.add(subqueryPanel, "SUBQUERY");
            
            add(valuePanel);
            
            // Show appropriate panel
            if (isSubquery) {
                valueCardLayout.show(valuePanel, "SUBQUERY");
                operatorCombo.setSelectedItem(WhereOperator.IN);
            } else {
                updateValuePanel();
            }
            
            // Remove button
            JButton removeButton = new JButton("✕");
            removeButton.setPreferredSize(new Dimension(30, 25));
            removeButton.setToolTipText("Remove this condition");
            removeButton.addActionListener(e -> removeConditionRow(this));
            add(removeButton);
        }

        private void updateValuePanel() {
            WhereOperator op = (WhereOperator) operatorCombo.getSelectedItem();
            if (op == null) return;
            
            if (isSubquery) {
                valueCardLayout.show(valuePanel, "SUBQUERY");
            } else if (!op.requiresValue()) {
                valueCardLayout.show(valuePanel, "NULL");
            } else if (op == WhereOperator.BETWEEN) {
                valueCardLayout.show(valuePanel, "BETWEEN");
            } else {
                valueCardLayout.show(valuePanel, "SIMPLE");
            }
        }

        private DocumentListener createDocListener() {
            return new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) { notifyChange(); }
                @Override
                public void removeUpdate(DocumentEvent e) { notifyChange(); }
                @Override
                public void changedUpdate(DocumentEvent e) { notifyChange(); }
            };
        }
        
        public void updateFieldSuggestions(List<String> suggestions) {
            fieldField.setSuggestions(suggestions);
        }

        public WhereCondition getCondition() {
            String field = fieldField.getText().trim();
            if (field.isEmpty()) {
                return null;
            }
            
            WhereCondition condition = new WhereCondition();
            condition.setField(field);
            condition.setOperator((WhereOperator) operatorCombo.getSelectedItem());
            condition.setLogicalOperator((LogicalOperator) logicalCombo.getSelectedItem());
            
            if (isSubquery) {
                condition.setSubquery(subqueryField.getText().trim());
            } else {
                condition.setValue(valueField.getText().trim());
                condition.setSecondValue(secondValueField.getText().trim());
            }
            
            return condition;
        }
    }
}
