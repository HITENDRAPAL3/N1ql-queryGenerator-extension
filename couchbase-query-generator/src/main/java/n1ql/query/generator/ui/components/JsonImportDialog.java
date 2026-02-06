package n1ql.query.generator.ui.components;

import n1ql.query.generator.services.JsonSchemaParser;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * Dialog for importing JSON sample documents to extract field suggestions.
 */
public class JsonImportDialog extends DialogWrapper {

    private JBTextArea jsonInputArea;
    private JBTextArea fieldsOutputArea;
    private List<String> extractedFields;
    private JBLabel statusLabel;

    public JsonImportDialog(Component parent) {
        super(parent, true);
        setTitle("Import JSON Schema");
        setSize(600, 500);
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(JBUI.Borders.empty(10));

        // Instructions
        JPanel instructionPanel = new JPanel(new BorderLayout());
        JBLabel instructionLabel = new JBLabel(
            "<html>Paste a sample JSON document to automatically extract field names.<br>" +
            "These fields will be available as suggestions in the query builder.</html>"
        );
        instructionLabel.setBorder(JBUI.Borders.emptyBottom(10));
        instructionPanel.add(instructionLabel, BorderLayout.CENTER);
        mainPanel.add(instructionPanel, BorderLayout.NORTH);

        // Split pane with JSON input and extracted fields
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.6);

        // Left side - JSON input
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("Sample JSON Document"));
        
        jsonInputArea = new JBTextArea(15, 30);
        jsonInputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        jsonInputArea.setText("{\n  \"name\": \"John Doe\",\n  \"email\": \"john@example.com\",\n  \"age\": 30,\n  \"address\": {\n    \"city\": \"New York\",\n    \"zip\": \"10001\"\n  },\n  \"tags\": [\"developer\", \"java\"]\n}");
        JBScrollPane inputScrollPane = new JBScrollPane(jsonInputArea);
        inputPanel.add(inputScrollPane, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton parseButton = new JButton("Parse JSON");
        parseButton.addActionListener(e -> parseJson());
        buttonPanel.add(parseButton);

        JButton formatButton = new JButton("Format");
        formatButton.addActionListener(e -> formatJson());
        buttonPanel.add(formatButton);

        JButton minifyButton = new JButton("Minify");
        minifyButton.addActionListener(e -> minifyJson());
        buttonPanel.add(minifyButton);

        inputPanel.add(buttonPanel, BorderLayout.SOUTH);
        splitPane.setLeftComponent(inputPanel);

        // Right side - Extracted fields
        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBorder(BorderFactory.createTitledBorder("Extracted Fields"));
        
        fieldsOutputArea = new JBTextArea(15, 20);
        fieldsOutputArea.setEditable(false);
        fieldsOutputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        JBScrollPane outputScrollPane = new JBScrollPane(fieldsOutputArea);
        outputPanel.add(outputScrollPane, BorderLayout.CENTER);

        splitPane.setRightComponent(outputPanel);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        // Status panel
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusLabel = new JBLabel(" ");
        statusLabel.setForeground(JBColor.GRAY);
        statusPanel.add(statusLabel, BorderLayout.WEST);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        // Initial parse
        parseJson();

        return mainPanel;
    }

    private void parseJson() {
        String json = jsonInputArea.getText();
        
        if (!JsonSchemaParser.isValidJson(json)) {
            statusLabel.setText("⚠️ Invalid JSON syntax");
            statusLabel.setForeground(JBColor.RED);
            fieldsOutputArea.setText("");
            extractedFields = null;
            return;
        }

        Map<String, String> fieldsWithTypes = JsonSchemaParser.extractFieldsWithTypes(json);
        extractedFields = JsonSchemaParser.extractFieldPaths(json);

        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(extractedFields.size()).append(" fields:\n\n");
        
        for (Map.Entry<String, String> entry : fieldsWithTypes.entrySet()) {
            sb.append(entry.getKey())
              .append(" : ")
              .append(entry.getValue())
              .append("\n");
        }

        fieldsOutputArea.setText(sb.toString());
        statusLabel.setText("✓ Parsed successfully - " + extractedFields.size() + " fields found");
        statusLabel.setForeground(JBColor.GREEN.darker());
    }

    private void formatJson() {
        String json = jsonInputArea.getText();
        String formatted = JsonSchemaParser.formatJson(json);
        jsonInputArea.setText(formatted);
        parseJson();
    }

    private void minifyJson() {
        String json = jsonInputArea.getText();
        String minified = JsonSchemaParser.minifyJson(json);
        jsonInputArea.setText(minified);
        parseJson();
    }

    public List<String> getExtractedFields() {
        return extractedFields;
    }

    public boolean hasValidFields() {
        return extractedFields != null && !extractedFields.isEmpty();
    }
}
