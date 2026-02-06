package n1ql.query.generator.ui.components;

import n1ql.query.generator.model.OrderByClause;
import n1ql.query.generator.model.SortOrder;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel for building ORDER BY clauses dynamically.
 */
public class OrderByPanel extends JBPanel<OrderByPanel> {
    
    private final List<OrderByRow> orderByRows;
    private final JPanel rowsContainer;
    private final Runnable onChangeCallback;
    private List<String> fieldSuggestions = new ArrayList<>();

    public OrderByPanel(Runnable onChangeCallback) {
        super(new BorderLayout());
        this.onChangeCallback = onChangeCallback;
        this.orderByRows = new ArrayList<>();
        
        setBorder(createTitledBorder("ORDER BY"));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        
        // Container for rows
        rowsContainer = new JPanel();
        rowsContainer.setLayout(new BoxLayout(rowsContainer, BoxLayout.Y_AXIS));
        
        JScrollPane scrollPane = new JScrollPane(rowsContainer);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setPreferredSize(new Dimension(0, 40)); // Width 0 = auto-adjust
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
        
        // Add button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("+ Add Sort Field");
        addButton.addActionListener(e -> addOrderByRow());
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

    public void addOrderByRow() {
        OrderByRow row = new OrderByRow();
        orderByRows.add(row);
        rowsContainer.add(row);
        rowsContainer.revalidate();
        rowsContainer.repaint();
        notifyChange();
    }

    public void removeOrderByRow(OrderByRow row) {
        orderByRows.remove(row);
        rowsContainer.remove(row);
        rowsContainer.revalidate();
        rowsContainer.repaint();
        notifyChange();
    }

    public List<OrderByClause> getOrderByClauses() {
        List<OrderByClause> clauses = new ArrayList<>();
        for (OrderByRow row : orderByRows) {
            OrderByClause clause = row.getClause();
            if (clause != null && clause.isValid()) {
                clauses.add(clause);
            }
        }
        return clauses;
    }

    public void reset() {
        orderByRows.clear();
        rowsContainer.removeAll();
        rowsContainer.revalidate();
        rowsContainer.repaint();
        notifyChange();
    }
    
    /**
     * Updates field suggestions for all order by rows.
     */
    public void updateFieldSuggestions(List<String> suggestions) {
        this.fieldSuggestions = suggestions != null ? new ArrayList<>(suggestions) : new ArrayList<>();
        for (OrderByRow row : orderByRows) {
            row.updateFieldSuggestions(this.fieldSuggestions);
        }
    }

    private void notifyChange() {
        if (onChangeCallback != null) {
            onChangeCallback.run();
        }
    }

    /**
     * Inner class representing a single ORDER BY row.
     */
    private class OrderByRow extends JPanel {
        private final AutocompleteTextField fieldField;
        private final ComboBox<SortOrder> sortOrderCombo;

        public OrderByRow() {
            super(new FlowLayout(FlowLayout.LEFT, 5, 2));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
            
            // Field name with autocomplete
            fieldField = new AutocompleteTextField(15);
            fieldField.setToolTipText("Field name to sort by (press ↓ for suggestions)");
            fieldField.setSuggestions(fieldSuggestions);
            fieldField.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) { notifyChange(); }
                @Override
                public void removeUpdate(DocumentEvent e) { notifyChange(); }
                @Override
                public void changedUpdate(DocumentEvent e) { notifyChange(); }
            });
            add(fieldField);
            
            // Sort order
            sortOrderCombo = new ComboBox<>(SortOrder.values());
            sortOrderCombo.setPreferredSize(new Dimension(100, 25));
            sortOrderCombo.addActionListener(e -> notifyChange());
            add(sortOrderCombo);
            
            // Remove button
            JButton removeButton = new JButton("✕");
            removeButton.setPreferredSize(new Dimension(30, 25));
            removeButton.setToolTipText("Remove this sort field");
            removeButton.addActionListener(e -> removeOrderByRow(this));
            add(removeButton);
        }
        
        public void updateFieldSuggestions(List<String> suggestions) {
            fieldField.setSuggestions(suggestions);
        }

        public OrderByClause getClause() {
            String field = fieldField.getText().trim();
            if (field.isEmpty()) {
                return null;
            }
            
            OrderByClause clause = new OrderByClause();
            clause.setField(field);
            clause.setSortOrder((SortOrder) sortOrderCombo.getSelectedItem());
            return clause;
        }
    }
}
