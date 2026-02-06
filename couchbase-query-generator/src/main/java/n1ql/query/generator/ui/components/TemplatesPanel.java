package n1ql.query.generator.ui.components;

import n1ql.query.generator.model.QueryTemplate;
import n1ql.query.generator.services.TemplatesManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

/**
 * Panel for displaying and managing query templates.
 */
public class TemplatesPanel extends JBPanel<TemplatesPanel> {

    private final TemplatesManager templatesManager;
    private final Consumer<String> onTemplateSelected;
    private final ComboBox<String> categoryFilter;
    private final JPanel templatesListPanel;

    public TemplatesPanel(Consumer<String> onTemplateSelected) {
        super(new BorderLayout());
        this.onTemplateSelected = onTemplateSelected;
        this.templatesManager = TemplatesManager.getInstance();

        setBorder(createTitledBorder("Query Templates"));
        setPreferredSize(new Dimension(300, 400));

        // Top panel with category filter
        JPanel filterPanel = new JPanel(new BorderLayout(5, 0));
        filterPanel.setBorder(JBUI.Borders.empty(5));
        
        filterPanel.add(new JBLabel("Category:"), BorderLayout.WEST);
        categoryFilter = new ComboBox<>();
        categoryFilter.addItem("All");
        for (String category : templatesManager.getCategories()) {
            categoryFilter.addItem(category);
        }
        categoryFilter.addActionListener(e -> refreshTemplates());
        filterPanel.add(categoryFilter, BorderLayout.CENTER);
        
        add(filterPanel, BorderLayout.NORTH);

        // Templates list
        templatesListPanel = new JPanel();
        templatesListPanel.setLayout(new BoxLayout(templatesListPanel, BoxLayout.Y_AXIS));
        
        JBScrollPane scrollPane = new JBScrollPane(templatesListPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // Bottom panel with actions
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        
        JButton addButton = new JButton("+ New Template");
        addButton.addActionListener(e -> showAddTemplateDialog());
        bottomPanel.add(addButton);
        
        add(bottomPanel, BorderLayout.SOUTH);

        // Register for template updates
        templatesManager.addListener(this::refreshTemplates);

        // Initial load
        refreshTemplates();
    }

    private TitledBorder createTitledBorder(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(JBColor.border()),
            title
        );
        border.setTitleColor(JBColor.foreground());
        return border;
    }

    public void refreshTemplates() {
        templatesListPanel.removeAll();

        String selectedCategory = (String) categoryFilter.getSelectedItem();
        List<QueryTemplate> templates;
        
        if ("All".equals(selectedCategory) || selectedCategory == null) {
            templates = templatesManager.getAllTemplates();
        } else {
            templates = templatesManager.getTemplatesByCategory(selectedCategory);
        }

        if (templates.isEmpty()) {
            JBLabel emptyLabel = new JBLabel("No templates available");
            emptyLabel.setForeground(JBColor.GRAY);
            emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
            emptyLabel.setBorder(JBUI.Borders.empty(20));
            templatesListPanel.add(emptyLabel);
        } else {
            String currentCategory = null;
            for (QueryTemplate template : templates) {
                // Category header
                if ("All".equals(selectedCategory) && 
                    (currentCategory == null || !currentCategory.equals(template.getCategory()))) {
                    currentCategory = template.getCategory();
                    if (currentCategory != null) {
                        JBLabel categoryLabel = new JBLabel(currentCategory);
                        categoryLabel.setFont(categoryLabel.getFont().deriveFont(Font.BOLD, 11f));
                        categoryLabel.setForeground(JBColor.GRAY);
                        categoryLabel.setBorder(JBUI.Borders.empty(10, 5, 5, 5));
                        categoryLabel.setAlignmentX(LEFT_ALIGNMENT);
                        templatesListPanel.add(categoryLabel);
                    }
                }
                
                TemplateRow row = new TemplateRow(template);
                row.setAlignmentX(LEFT_ALIGNMENT);
                templatesListPanel.add(row);
                templatesListPanel.add(Box.createVerticalStrut(2));
            }
        }

        templatesListPanel.add(Box.createVerticalGlue());
        templatesListPanel.revalidate();
        templatesListPanel.repaint();
    }

    private void showAddTemplateDialog() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JBLabel("Name:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        JBTextField nameField = new JBTextField(20);
        panel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        panel.add(new JBLabel("Category:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        JBTextField categoryField = new JBTextField(20);
        categoryField.setText("Custom");
        panel.add(categoryField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        panel.add(new JBLabel("Description:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        JBTextField descField = new JBTextField(20);
        panel.add(descField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JBLabel("Query:"), gbc);

        gbc.gridy = 4;
        JBTextArea queryArea = new JBTextArea(8, 30);
        queryArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JBScrollPane scrollPane = new JBScrollPane(queryArea);
        panel.add(scrollPane, gbc);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add New Template",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String query = queryArea.getText().trim();
            
            if (!name.isEmpty() && !query.isEmpty()) {
                templatesManager.addTemplate(
                    name,
                    query,
                    descField.getText().trim(),
                    categoryField.getText().trim()
                );
                refreshTemplates();
            } else {
                JOptionPane.showMessageDialog(this,
                    "Name and Query are required.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Row representing a single template.
     */
    private class TemplateRow extends JPanel {
        private final QueryTemplate template;
        private boolean isHovered = false;

        public TemplateRow(QueryTemplate template) {
            super(new BorderLayout(5, 2));
            this.template = template;
            
            setBorder(JBUI.Borders.empty(5, 8));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            // Left side - template info
            JPanel infoPanel = new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.setOpaque(false);

            JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            namePanel.setOpaque(false);
            
            JBLabel nameLabel = new JBLabel(template.getName());
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 12f));
            namePanel.add(nameLabel);
            
            if (template.isBuiltIn()) {
                JBLabel builtInLabel = new JBLabel("[built-in]");
                builtInLabel.setFont(builtInLabel.getFont().deriveFont(Font.ITALIC, 9f));
                builtInLabel.setForeground(JBColor.GRAY);
                namePanel.add(builtInLabel);
            }
            
            infoPanel.add(namePanel);

            if (template.getDescription() != null && !template.getDescription().isEmpty()) {
                JBLabel descLabel = new JBLabel(template.getDescription());
                descLabel.setFont(descLabel.getFont().deriveFont(Font.PLAIN, 10f));
                descLabel.setForeground(JBColor.GRAY);
                infoPanel.add(descLabel);
            }

            add(infoPanel, BorderLayout.CENTER);

            // Right side - actions (only for custom templates)
            if (!template.isBuiltIn()) {
                JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
                actionsPanel.setOpaque(false);

                JButton deleteButton = new JButton("✕");
                deleteButton.setPreferredSize(new Dimension(30, 25));
                deleteButton.setToolTipText("Delete template");
                deleteButton.addActionListener(e -> {
                    int confirm = JOptionPane.showConfirmDialog(this,
                        "Delete template '" + template.getName() + "'?",
                        "Confirm Delete",
                        JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        templatesManager.deleteTemplate(template.getId());
                        refreshTemplates();
                    }
                });
                actionsPanel.add(deleteButton);

                add(actionsPanel, BorderLayout.EAST);
            }

            // Hover effect and click
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
                    if (e.getClickCount() == 2 && onTemplateSelected != null) {
                        templatesManager.recordUsage(template.getId());
                        onTemplateSelected.accept(template.getQuery());
                    }
                }
            });

            // Tooltip with query preview
            setToolTipText("<html><pre>" + escapeHtml(template.getQuery()) + "</pre></html>");
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
