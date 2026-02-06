package n1ql.query.generator.ui.components;

import com.intellij.ui.components.JBTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TextField with autocomplete dropdown for field suggestions.
 */
public class AutocompleteTextField extends JBTextField {

    private final JPopupMenu suggestionPopup;
    private final JList<String> suggestionList;
    private final DefaultListModel<String> listModel;
    private List<String> allSuggestions;
    private boolean isPopupVisible = false;
    private boolean isInternalUpdate = false;

    public AutocompleteTextField(int columns) {
        super(columns);
        this.allSuggestions = new ArrayList<>();
        this.listModel = new DefaultListModel<>();
        this.suggestionList = new JList<>(listModel);
        this.suggestionPopup = new JPopupMenu();
        
        setupAutocomplete();
    }

    public AutocompleteTextField(int columns, List<String> suggestions) {
        this(columns);
        setSuggestions(suggestions);
    }

    private void setupAutocomplete() {
        // Configure suggestion list
        suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestionList.setVisibleRowCount(8);
        suggestionList.setFont(getFont());
        
        // Add scrollpane to popup
        JScrollPane scrollPane = new JScrollPane(suggestionList);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        scrollPane.setPreferredSize(new Dimension(300, 150));
        suggestionPopup.add(scrollPane);
        suggestionPopup.setBorder(BorderFactory.createEmptyBorder());
        
        // Listen to text changes
        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateSuggestions();
            }
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                updateSuggestions();
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                updateSuggestions();
            }
        });
        
        // Handle keyboard navigation
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!isPopupVisible) {
                    if (e.getKeyCode() == KeyEvent.VK_DOWN && !allSuggestions.isEmpty()) {
                        showSuggestions();
                        e.consume();
                    }
                    return;
                }
                
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_DOWN:
                        int nextIndex = suggestionList.getSelectedIndex() + 1;
                        if (nextIndex < listModel.getSize()) {
                            suggestionList.setSelectedIndex(nextIndex);
                            suggestionList.ensureIndexIsVisible(nextIndex);
                        }
                        e.consume();
                        break;
                        
                    case KeyEvent.VK_UP:
                        int prevIndex = suggestionList.getSelectedIndex() - 1;
                        if (prevIndex >= 0) {
                            suggestionList.setSelectedIndex(prevIndex);
                            suggestionList.ensureIndexIsVisible(prevIndex);
                        }
                        e.consume();
                        break;
                        
                    case KeyEvent.VK_ENTER:
                        if (suggestionList.getSelectedIndex() >= 0) {
                            acceptSuggestion();
                            e.consume();
                        }
                        break;
                        
                    case KeyEvent.VK_ESCAPE:
                        hideSuggestions();
                        e.consume();
                        break;
                }
            }
        });
        
        // Handle mouse selection
        suggestionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    acceptSuggestion();
                }
            }
        });
        
        // Hide popup when focus is lost
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                // Only hide if focus is not moving to the popup or suggestion list
                Component opposite = e.getOppositeComponent();
                if (opposite != suggestionList && opposite != suggestionPopup) {
                    // Delay hiding to allow mouse clicks on the list to process
                    SwingUtilities.invokeLater(() -> {
                        // Double-check: only hide if popup doesn't have focus
                        if (!suggestionList.hasFocus() && !isFocusOwner()) {
                            hideSuggestions();
                        }
                    });
                }
            }
        });
    }

    public void setSuggestions(List<String> suggestions) {
        this.allSuggestions = suggestions != null ? new ArrayList<>(suggestions) : new ArrayList<>();
    }

    public void updateSuggestions(List<String> suggestions) {
        setSuggestions(suggestions);
        updateSuggestions();
    }

    private void updateSuggestions() {
        String input = getText().trim();
        
        if (input.isEmpty() || allSuggestions.isEmpty()) {
            hideSuggestions();
            return;
        }
        
        // Filter suggestions based on input
        String lowerInput = input.toLowerCase();
        List<String> filtered = allSuggestions.stream()
            .filter(s -> s.toLowerCase().contains(lowerInput))
            .sorted((a, b) -> {
                // Prioritize prefix matches
                boolean aStarts = a.toLowerCase().startsWith(lowerInput);
                boolean bStarts = b.toLowerCase().startsWith(lowerInput);
                if (aStarts && !bStarts) return -1;
                if (!aStarts && bStarts) return 1;
                return a.compareToIgnoreCase(b);
            })
            .limit(50) // Limit results
            .collect(Collectors.toList());
        
        if (filtered.isEmpty()) {
            hideSuggestions();
            return;
        }
        
        // Update list model
        listModel.clear();
        filtered.forEach(listModel::addElement);
        suggestionList.setSelectedIndex(0);
        
        showSuggestions();
    }

    private void showSuggestions() {
        if (listModel.isEmpty()) {
            return;
        }
        
        if (!isPopupVisible) {
            try {
                suggestionPopup.show(this, 0, getHeight());
                isPopupVisible = true;
            } catch (IllegalComponentStateException e) {
                // Component not showing, ignore
            }
        }
    }

    private void hideSuggestions() {
        if (isPopupVisible) {
            suggestionPopup.setVisible(false);
            isPopupVisible = false;
        }
    }

    private void acceptSuggestion() {
        String selected = suggestionList.getSelectedValue();
        if (selected != null) {
            isInternalUpdate = true;  // Mark as internal
            setText(selected);
            isInternalUpdate = false;  // Reset
            setCaretPosition(selected.length());
            // Popup stays open, will update when user continues typing
        }
    }

    @Override
    public void setText(String text) {
        super.setText(text);
        // Only hide if this is an external/programmatic call
        if (!isInternalUpdate) {
            hideSuggestions();
        }
    }
}
