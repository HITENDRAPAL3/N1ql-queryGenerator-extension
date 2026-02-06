package n1ql.query.generator.ui.components;

import n1ql.query.generator.model.SetClause;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
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
 * Panel for building SET clauses for UPDATE operations.
 */
public class SetClausePanel extends JBPanel<SetClausePanel> {
    
    private final List<SetClauseRow> setClauseRows;
    private final JPanel rowsContainer;
    private final Runnable onChangeCallback;
    private List<String> fieldSuggestions = new ArrayList<>();

    public SetClausePanel(Runnable onChangeCallback) {
        super(new BorderLayout());
        this.onChangeCallback = onChangeCallback;
        this.setClauseRows = new ArrayList<>();
        
        setBorder(createTitledBorder("SET Clause"));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        
        // Container for rows
        rowsContainer = new JPanel();
        rowsContainer.setLayout(new BoxLayout(rowsContainer, BoxLayout.Y_AXIS));
        
        JScrollPane scrollPane = new JScrollPane(rowsContainer);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setPreferredSize(new Dimension(0, 50)); // Width 0 = auto-adjust
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
        
        // Add button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("+ Add Field");
        addButton.addActionListener(e -> addSetClauseRow());
        buttonPanel.add(addButton);
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

    public void addSetClauseRow() {
        SetClauseRow row = new SetClauseRow();
        setClauseRows.add(row);
        rowsContainer.add(row);
        rowsContainer.revalidate();
        rowsContainer.repaint();
        notifyChange();
    }

    public void removeSetClauseRow(SetClauseRow row) {
        setClauseRows.remove(row);
        rowsContainer.remove(row);
        rowsContainer.revalidate();
        rowsContainer.repaint();
        notifyChange();
    }

    public List<SetClause> getSetClauses() {
        List<SetClause> clauses = new ArrayList<>();
        for (SetClauseRow row : setClauseRows) {
            SetClause clause = row.getClause();
            if (clause != null && clause.isValid()) {
                clauses.add(clause);
            }
        }
        return clauses;
    }

    public void reset() {
        setClauseRows.clear();
        rowsContainer.removeAll();
        rowsContainer.revalidate();
        rowsContainer.repaint();
        notifyChange();
    }
    
    /**
     * Updates field suggestions for all set clause rows.
     */
    public void updateFieldSuggestions(List<String> suggestions) {
        this.fieldSuggestions = suggestions != null ? new ArrayList<>(suggestions) : new ArrayList<>();
        for (SetClauseRow row : setClauseRows) {
            row.updateFieldSuggestions(this.fieldSuggestions);
        }
    }

    private void notifyChange() {
        if (onChangeCallback != null) {
            onChangeCallback.run();
        }
    }

    /**
     * Inner class representing a single SET clause row.
     */
    private class SetClauseRow extends JPanel {
        private final AutocompleteTextField fieldField;
        private final JBTextField valueField;
        private final JBCheckBox expressionCheckbox;

        public SetClauseRow() {
            super(new FlowLayout(FlowLayout.LEFT, 5, 2));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
            
            // Field name with autocomplete
            fieldField = new AutocompleteTextField(12);
            fieldField.setToolTipText("Field name to update (press ↓ for suggestions)");
            fieldField.setSuggestions(fieldSuggestions);
            fieldField.getDocument().addDocumentListener(createDocListener());
            add(fieldField);
            
            add(new JLabel(" = "));
            
            // Value
            valueField = new JBTextField(15);
            valueField.setToolTipText("New value");
            valueField.getDocument().addDocumentListener(createDocListener());
            add(valueField);
            
            // Expression checkbox
            expressionCheckbox = new JBCheckBox("Expression");
            expressionCheckbox.setToolTipText("Check if value is an expression (e.g., field + 1)");
            expressionCheckbox.addActionListener(e -> notifyChange());
            add(expressionCheckbox);
            
            // Remove button
            JButton removeButton = new JButton("✕");
            removeButton.setPreferredSize(new Dimension(30, 25));
            removeButton.setToolTipText("Remove this field");
            removeButton.addActionListener(e -> removeSetClauseRow(this));
            add(removeButton);
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

        public SetClause getClause() {
            String field = fieldField.getText().trim();
            String value = valueField.getText().trim();
            
            if (field.isEmpty()) {
                return null;
            }
            
            SetClause clause = new SetClause();
            clause.setField(field);
            clause.setValue(value);
            clause.setExpression(expressionCheckbox.isSelected());
            return clause;
        }
    }
}
