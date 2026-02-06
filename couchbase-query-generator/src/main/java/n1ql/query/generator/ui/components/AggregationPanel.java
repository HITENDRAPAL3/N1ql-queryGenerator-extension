package n1ql.query.generator.ui.components;

import n1ql.query.generator.model.*;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Panel for building aggregation queries (GROUP BY, aggregate functions, HAVING).
 */
public class AggregationPanel extends JBPanel<AggregationPanel> {

    private final List<AggregationRow> aggregationRows;
    private final JPanel aggregationsContainer;
    private final JBTextField groupByField;
    private final List<HavingConditionRow> havingRows;
    private final JPanel havingContainer;
    private final Runnable onChangeCallback;
    private final JBCheckBox enableAggregationCheckbox;
    private List<String> fieldSuggestions = new ArrayList<>();

    public AggregationPanel(Runnable onChangeCallback) {
        super(new BorderLayout());
        this.onChangeCallback = onChangeCallback;
        this.aggregationRows = new ArrayList<>();
        this.havingRows = new ArrayList<>();

        setBorder(createTitledBorder("Aggregation (GROUP BY)"));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 350));

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        // Enable checkbox
        JPanel enablePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        enableAggregationCheckbox = new JBCheckBox("Enable Aggregation", false);
        enableAggregationCheckbox.addActionListener(e -> {
            updateEnabledState();
            notifyChange();
        });
        enablePanel.add(enableAggregationCheckbox);
        contentPanel.add(enablePanel);

        // Aggregate functions section
        JPanel aggSection = new JPanel(new BorderLayout());
        aggSection.setBorder(BorderFactory.createTitledBorder("Aggregate Functions"));
        
        aggregationsContainer = new JPanel();
        aggregationsContainer.setLayout(new BoxLayout(aggregationsContainer, BoxLayout.Y_AXIS));
        JScrollPane aggScrollPane = new JScrollPane(aggregationsContainer);
        aggScrollPane.setPreferredSize(new Dimension(0, 40)); // Width 0 = auto-adjust
        aggScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        aggScrollPane.setBorder(JBUI.Borders.empty());
        aggSection.add(aggScrollPane, BorderLayout.CENTER);

        JPanel aggButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addAggButton = new JButton("+ Add Aggregate");
        addAggButton.addActionListener(e -> addAggregationRow());
        aggButtonPanel.add(addAggButton);
        aggSection.add(aggButtonPanel, BorderLayout.SOUTH);

        contentPanel.add(aggSection);

        // GROUP BY section
        JPanel groupByPanel = new JPanel(new BorderLayout());
        groupByPanel.setBorder(BorderFactory.createTitledBorder("GROUP BY Fields"));
        groupByPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        groupByField = new JBTextField();
        groupByField.setToolTipText("Comma-separated field names (e.g., status, category)");
        groupByField.getDocument().addDocumentListener(createDocListener());
        groupByPanel.add(groupByField, BorderLayout.CENTER);

        contentPanel.add(groupByPanel);

        // HAVING section
        JPanel havingSection = new JPanel(new BorderLayout());
        havingSection.setBorder(BorderFactory.createTitledBorder("HAVING Conditions"));

        havingContainer = new JPanel();
        havingContainer.setLayout(new BoxLayout(havingContainer, BoxLayout.Y_AXIS));
        JScrollPane havingScrollPane = new JScrollPane(havingContainer);
        havingScrollPane.setPreferredSize(new Dimension(0, 30)); // Width 0 = auto-adjust
        havingScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        havingScrollPane.setBorder(JBUI.Borders.empty());
        havingSection.add(havingScrollPane, BorderLayout.CENTER);

        JPanel havingButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addHavingButton = new JButton("+ Add HAVING Condition");
        addHavingButton.addActionListener(e -> addHavingRow());
        havingButtonPanel.add(addHavingButton);
        havingSection.add(havingButtonPanel, BorderLayout.SOUTH);

        contentPanel.add(havingSection);

        add(contentPanel, BorderLayout.CENTER);

        // Initially disabled
        updateEnabledState();
    }

    private TitledBorder createTitledBorder(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(JBColor.border()),
            title
        );
        border.setTitleColor(JBColor.foreground());
        return border;
    }

    private void updateEnabledState() {
        boolean enabled = enableAggregationCheckbox.isSelected();
        aggregationsContainer.setEnabled(enabled);
        groupByField.setEnabled(enabled);
        havingContainer.setEnabled(enabled);
        
        for (Component c : aggregationsContainer.getComponents()) {
            setEnabledRecursive(c, enabled);
        }
        for (Component c : havingContainer.getComponents()) {
            setEnabledRecursive(c, enabled);
        }
    }

    private void setEnabledRecursive(Component component, boolean enabled) {
        component.setEnabled(enabled);
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                setEnabledRecursive(child, enabled);
            }
        }
    }

    public void addAggregationRow() {
        AggregationRow row = new AggregationRow();
        aggregationRows.add(row);
        aggregationsContainer.add(row);
        aggregationsContainer.revalidate();
        aggregationsContainer.repaint();
        notifyChange();
    }

    public void removeAggregationRow(AggregationRow row) {
        aggregationRows.remove(row);
        aggregationsContainer.remove(row);
        aggregationsContainer.revalidate();
        aggregationsContainer.repaint();
        notifyChange();
    }

    public void addHavingRow() {
        HavingConditionRow row = new HavingConditionRow();
        havingRows.add(row);
        havingContainer.add(row);
        havingContainer.revalidate();
        havingContainer.repaint();
        notifyChange();
    }

    public void removeHavingRow(HavingConditionRow row) {
        havingRows.remove(row);
        havingContainer.remove(row);
        havingContainer.revalidate();
        havingContainer.repaint();
        notifyChange();
    }

    public boolean isAggregationEnabled() {
        return enableAggregationCheckbox.isSelected();
    }

    public List<AggregationClause> getAggregations() {
        if (!isAggregationEnabled()) {
            return new ArrayList<>();
        }
        List<AggregationClause> clauses = new ArrayList<>();
        for (AggregationRow row : aggregationRows) {
            AggregationClause clause = row.getClause();
            if (clause != null && clause.isValid()) {
                clauses.add(clause);
            }
        }
        return clauses;
    }

    public List<String> getGroupByFields() {
        if (!isAggregationEnabled()) {
            return new ArrayList<>();
        }
        String text = groupByField.getText().trim();
        if (text.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(text.split("\\s*,\\s*"));
    }

    public List<WhereCondition> getHavingConditions() {
        if (!isAggregationEnabled()) {
            return new ArrayList<>();
        }
        List<WhereCondition> conditions = new ArrayList<>();
        for (HavingConditionRow row : havingRows) {
            WhereCondition condition = row.getCondition();
            if (condition != null) {
                conditions.add(condition);
            }
        }
        return conditions;
    }

    public void reset() {
        enableAggregationCheckbox.setSelected(false);
        aggregationRows.clear();
        aggregationsContainer.removeAll();
        groupByField.setText("");
        havingRows.clear();
        havingContainer.removeAll();
        aggregationsContainer.revalidate();
        aggregationsContainer.repaint();
        havingContainer.revalidate();
        havingContainer.repaint();
        updateEnabledState();
        notifyChange();
    }
    
    /**
     * Updates field suggestions for all aggregation and having rows.
     */
    public void updateFieldSuggestions(List<String> suggestions) {
        this.fieldSuggestions = suggestions != null ? new ArrayList<>(suggestions) : new ArrayList<>();
        for (AggregationRow row : aggregationRows) {
            row.updateFieldSuggestions(this.fieldSuggestions);
        }
        for (HavingConditionRow row : havingRows) {
            row.updateFieldSuggestions(this.fieldSuggestions);
        }
    }

    private void notifyChange() {
        if (onChangeCallback != null) {
            onChangeCallback.run();
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

    /**
     * Row for aggregate function selection.
     */
    private class AggregationRow extends JPanel {
        private final ComboBox<AggregateFunction> functionCombo;
        private final AutocompleteTextField fieldField;
        private final JBTextField aliasField;

        public AggregationRow() {
            super(new FlowLayout(FlowLayout.LEFT, 5, 2));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

            // Function selector
            functionCombo = new ComboBox<>(AggregateFunction.values());
            functionCombo.setPreferredSize(new Dimension(150, 25));
            functionCombo.addActionListener(e -> notifyChange());
            add(functionCombo);

            add(new JLabel("("));

            // Field with autocomplete
            fieldField = new AutocompleteTextField(10);
            fieldField.setToolTipText("Field name (or * for COUNT, press ↓ for suggestions)");
            fieldField.setSuggestions(fieldSuggestions);
            fieldField.getDocument().addDocumentListener(createDocListener());
            add(fieldField);

            add(new JLabel(")"));

            add(new JLabel(" AS "));

            // Alias
            aliasField = new JBTextField(8);
            aliasField.setToolTipText("Alias (optional)");
            aliasField.getDocument().addDocumentListener(createDocListener());
            add(aliasField);

            // Remove button
            JButton removeButton = new JButton("✕");
            removeButton.setPreferredSize(new Dimension(30, 25));
            removeButton.addActionListener(e -> removeAggregationRow(this));
            add(removeButton);
        }
        
        public void updateFieldSuggestions(List<String> suggestions) {
            fieldField.setSuggestions(suggestions);
        }

        public AggregationClause getClause() {
            AggregateFunction function = (AggregateFunction) functionCombo.getSelectedItem();
            if (function == null) return null;

            AggregationClause clause = new AggregationClause();
            clause.setFunction(function);
            clause.setField(fieldField.getText().trim());
            clause.setAlias(aliasField.getText().trim());
            return clause;
        }
    }

    /**
     * Row for HAVING condition.
     */
    private class HavingConditionRow extends JPanel {
        private final ComboBox<LogicalOperator> logicalCombo;
        private final AutocompleteTextField fieldField;
        private final ComboBox<WhereOperator> operatorCombo;
        private final JBTextField valueField;

        public HavingConditionRow() {
            super(new FlowLayout(FlowLayout.LEFT, 5, 2));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

            // Logical operator
            logicalCombo = new ComboBox<>(LogicalOperator.values());
            logicalCombo.setPreferredSize(new Dimension(60, 25));
            logicalCombo.addActionListener(e -> notifyChange());
            if (havingRows.isEmpty()) {
                logicalCombo.setVisible(false);
            }
            add(logicalCombo);

            // Field (typically an aggregate function like COUNT(*)) with autocomplete
            fieldField = new AutocompleteTextField(12);
            fieldField.setToolTipText("Aggregate expression (e.g., COUNT(*), SUM(amount), press ↓ for suggestions)");
            fieldField.setSuggestions(fieldSuggestions);
            fieldField.getDocument().addDocumentListener(createDocListener());
            add(fieldField);

            // Operator
            operatorCombo = new ComboBox<>(new WhereOperator[]{
                WhereOperator.EQUALS, WhereOperator.NOT_EQUALS,
                WhereOperator.GREATER_THAN, WhereOperator.LESS_THAN,
                WhereOperator.GREATER_THAN_OR_EQUALS, WhereOperator.LESS_THAN_OR_EQUALS
            });
            operatorCombo.setPreferredSize(new Dimension(80, 25));
            operatorCombo.addActionListener(e -> notifyChange());
            add(operatorCombo);

            // Value
            valueField = new JBTextField(8);
            valueField.setToolTipText("Value to compare");
            valueField.getDocument().addDocumentListener(createDocListener());
            add(valueField);

            // Remove button
            JButton removeButton = new JButton("✕");
            removeButton.setPreferredSize(new Dimension(30, 25));
            removeButton.addActionListener(e -> removeHavingRow(this));
            add(removeButton);
        }
        
        public void updateFieldSuggestions(List<String> suggestions) {
            fieldField.setSuggestions(suggestions);
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
