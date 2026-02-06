package n1ql.query.generator.ui.highlighting;

import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Syntax highlighter for N1QL queries.
 * Provides color-coded highlighting for keywords, strings, numbers, and comments.
 */
public class N1QLSyntaxHighlighter {

    // N1QL Keywords
    private static final Set<String> KEYWORDS = new HashSet<>();
    private static final Set<String> FUNCTIONS = new HashSet<>();
    private static final Set<String> OPERATORS = new HashSet<>();

    static {
        // DML Keywords
        KEYWORDS.add("SELECT");
        KEYWORDS.add("FROM");
        KEYWORDS.add("WHERE");
        KEYWORDS.add("AND");
        KEYWORDS.add("OR");
        KEYWORDS.add("NOT");
        KEYWORDS.add("IN");
        KEYWORDS.add("LIKE");
        KEYWORDS.add("BETWEEN");
        KEYWORDS.add("IS");
        KEYWORDS.add("NULL");
        KEYWORDS.add("TRUE");
        KEYWORDS.add("FALSE");
        KEYWORDS.add("AS");
        KEYWORDS.add("ORDER");
        KEYWORDS.add("BY");
        KEYWORDS.add("ASC");
        KEYWORDS.add("DESC");
        KEYWORDS.add("LIMIT");
        KEYWORDS.add("OFFSET");
        KEYWORDS.add("GROUP");
        KEYWORDS.add("HAVING");
        KEYWORDS.add("DISTINCT");
        KEYWORDS.add("ALL");
        KEYWORDS.add("JOIN");
        KEYWORDS.add("LEFT");
        KEYWORDS.add("RIGHT");
        KEYWORDS.add("INNER");
        KEYWORDS.add("OUTER");
        KEYWORDS.add("ON");
        KEYWORDS.add("INSERT");
        KEYWORDS.add("INTO");
        KEYWORDS.add("VALUES");
        KEYWORDS.add("UPDATE");
        KEYWORDS.add("SET");
        KEYWORDS.add("DELETE");
        KEYWORDS.add("UPSERT");
        KEYWORDS.add("MERGE");
        KEYWORDS.add("RETURNING");
        KEYWORDS.add("KEY");
        KEYWORDS.add("VALUE");
        KEYWORDS.add("USE");
        KEYWORDS.add("KEYS");
        KEYWORDS.add("NEST");
        KEYWORDS.add("UNNEST");
        KEYWORDS.add("LET");
        KEYWORDS.add("LETTING");
        KEYWORDS.add("WITH");
        KEYWORDS.add("UNION");
        KEYWORDS.add("INTERSECT");
        KEYWORDS.add("EXCEPT");
        KEYWORDS.add("ANY");
        KEYWORDS.add("EVERY");
        KEYWORDS.add("SATISFIES");
        KEYWORDS.add("END");
        KEYWORDS.add("WHEN");
        KEYWORDS.add("THEN");
        KEYWORDS.add("ELSE");
        KEYWORDS.add("CASE");
        KEYWORDS.add("EXISTS");
        KEYWORDS.add("MISSING");
        KEYWORDS.add("CONTAINS");

        // Aggregate Functions
        FUNCTIONS.add("COUNT");
        FUNCTIONS.add("SUM");
        FUNCTIONS.add("AVG");
        FUNCTIONS.add("MIN");
        FUNCTIONS.add("MAX");
        FUNCTIONS.add("ARRAY_AGG");
        FUNCTIONS.add("ARRAY_LENGTH");
        FUNCTIONS.add("ARRAY_CONCAT");
        FUNCTIONS.add("ARRAY_CONTAINS");
        FUNCTIONS.add("ARRAY_DISTINCT");
        FUNCTIONS.add("ARRAY_FLATTEN");

        // String Functions
        FUNCTIONS.add("LOWER");
        FUNCTIONS.add("UPPER");
        FUNCTIONS.add("TRIM");
        FUNCTIONS.add("LTRIM");
        FUNCTIONS.add("RTRIM");
        FUNCTIONS.add("LENGTH");
        FUNCTIONS.add("SUBSTR");
        FUNCTIONS.add("CONCAT");
        FUNCTIONS.add("REPLACE");
        FUNCTIONS.add("SPLIT");
        FUNCTIONS.add("REGEXP_LIKE");
        FUNCTIONS.add("REGEXP_CONTAINS");

        // Type Functions
        FUNCTIONS.add("TYPE");
        FUNCTIONS.add("TOSTRING");
        FUNCTIONS.add("TONUMBER");
        FUNCTIONS.add("TOBOOLEAN");
        FUNCTIONS.add("TOARRAY");
        FUNCTIONS.add("TOOBJECT");

        // Date Functions
        FUNCTIONS.add("NOW_STR");
        FUNCTIONS.add("NOW_MILLIS");
        FUNCTIONS.add("DATE_ADD_STR");
        FUNCTIONS.add("DATE_DIFF_STR");
        FUNCTIONS.add("DATE_PART_STR");
        FUNCTIONS.add("STR_TO_MILLIS");
        FUNCTIONS.add("MILLIS_TO_STR");

        // Other Functions
        FUNCTIONS.add("META");
        FUNCTIONS.add("UUID");
        FUNCTIONS.add("IFNULL");
        FUNCTIONS.add("IFMISSING");
        FUNCTIONS.add("IFMISSINGORNULL");
        FUNCTIONS.add("COALESCE");
        FUNCTIONS.add("NULLIF");
        FUNCTIONS.add("OBJECT_LENGTH");
        FUNCTIONS.add("OBJECT_NAMES");
        FUNCTIONS.add("OBJECT_VALUES");
        FUNCTIONS.add("OBJECT_PAIRS");

        // Operators
        OPERATORS.add("=");
        OPERATORS.add("!=");
        OPERATORS.add("<>");
        OPERATORS.add(">");
        OPERATORS.add("<");
        OPERATORS.add(">=");
        OPERATORS.add("<=");
    }

    // Colors for different token types
    private final Color keywordColor;
    private final Color functionColor;
    private final Color stringColor;
    private final Color numberColor;
    private final Color commentColor;
    private final Color operatorColor;
    private final Color defaultColor;
    private final Color bucketColor;

    public N1QLSyntaxHighlighter() {
        // Define colors that work in both light and dark themes
        this.keywordColor = new JBColor(new Color(0, 0, 180), new Color(204, 120, 50));      // Blue / Orange
        this.functionColor = new JBColor(new Color(128, 0, 128), new Color(255, 198, 109)); // Purple / Gold
        this.stringColor = new JBColor(new Color(0, 128, 0), new Color(106, 135, 89));      // Green
        this.numberColor = new JBColor(new Color(255, 0, 0), new Color(104, 151, 187));     // Red / Blue
        this.commentColor = new JBColor(new Color(128, 128, 128), new Color(128, 128, 128)); // Gray
        this.operatorColor = new JBColor(new Color(0, 128, 128), new Color(169, 183, 198)); // Teal / Light gray
        this.defaultColor = JBColor.foreground();
        this.bucketColor = new JBColor(new Color(139, 69, 19), new Color(152, 118, 170));   // Brown / Light purple
    }

    /**
     * Applies syntax highlighting to a JTextPane.
     */
    public void highlight(JTextPane textPane) {
        StyledDocument doc = textPane.getStyledDocument();
        String text = textPane.getText();

        // Clear existing styles
        Style defaultStyle = createStyle(doc, "default", defaultColor, false, false);
        doc.setCharacterAttributes(0, text.length(), defaultStyle, true);

        // Apply highlighting in order
        highlightComments(doc, text);
        highlightStrings(doc, text);
        highlightBucketNames(doc, text);
        highlightKeywords(doc, text);
        highlightFunctions(doc, text);
        highlightNumbers(doc, text);
        
        // Ensure editability is preserved after highlighting
        textPane.setEditable(true);
    }

    private Style createStyle(StyledDocument doc, String name, Color color, boolean bold, boolean italic) {
        Style style = doc.addStyle(name, null);
        StyleConstants.setForeground(style, color);
        StyleConstants.setBold(style, bold);
        StyleConstants.setItalic(style, italic);
        return style;
    }

    private void highlightComments(StyledDocument doc, String text) {
        Style commentStyle = createStyle(doc, "comment", commentColor, false, true);

        // Single-line comments: -- or //
        Pattern singleLinePattern = Pattern.compile("(--|//).*$", Pattern.MULTILINE);
        Matcher matcher = singleLinePattern.matcher(text);
        while (matcher.find()) {
            doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), commentStyle, true);
        }

        // Multi-line comments: /* */
        Pattern multiLinePattern = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
        matcher = multiLinePattern.matcher(text);
        while (matcher.find()) {
            doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), commentStyle, true);
        }
    }

    private void highlightStrings(StyledDocument doc, String text) {
        Style stringStyle = createStyle(doc, "string", stringColor, false, false);

        // Double-quoted strings
        Pattern doubleQuotePattern = Pattern.compile("\"([^\"\\\\]|\\\\.)*\"");
        Matcher matcher = doubleQuotePattern.matcher(text);
        while (matcher.find()) {
            doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), stringStyle, true);
        }

        // Single-quoted strings
        Pattern singleQuotePattern = Pattern.compile("'([^'\\\\]|\\\\.)*'");
        matcher = singleQuotePattern.matcher(text);
        while (matcher.find()) {
            doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), stringStyle, true);
        }
    }

    private void highlightBucketNames(StyledDocument doc, String text) {
        Style bucketStyle = createStyle(doc, "bucket", bucketColor, false, false);

        // Backtick-quoted identifiers (bucket/scope/collection names)
        Pattern backtickPattern = Pattern.compile("`[^`]+`");
        Matcher matcher = backtickPattern.matcher(text);
        while (matcher.find()) {
            doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), bucketStyle, true);
        }
    }

    private void highlightKeywords(StyledDocument doc, String text) {
        Style keywordStyle = createStyle(doc, "keyword", keywordColor, true, false);

        for (String keyword : KEYWORDS) {
            Pattern pattern = Pattern.compile("\\b" + keyword + "\\b", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                // Don't highlight if inside a string or comment
                if (!isInsideStringOrComment(text, matcher.start())) {
                    doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), keywordStyle, true);
                }
            }
        }
    }

    private void highlightFunctions(StyledDocument doc, String text) {
        Style functionStyle = createStyle(doc, "function", functionColor, false, false);

        for (String function : FUNCTIONS) {
            // Match function name followed by opening parenthesis
            Pattern pattern = Pattern.compile("\\b" + function + "\\s*\\(", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                if (!isInsideStringOrComment(text, matcher.start())) {
                    // Highlight only the function name, not the parenthesis
                    int endPos = matcher.end() - 1;
                    while (endPos > matcher.start() && (text.charAt(endPos) == '(' || Character.isWhitespace(text.charAt(endPos)))) {
                        endPos--;
                    }
                    doc.setCharacterAttributes(matcher.start(), endPos - matcher.start() + 1, functionStyle, true);
                }
            }
        }
    }

    private void highlightNumbers(StyledDocument doc, String text) {
        Style numberStyle = createStyle(doc, "number", numberColor, false, false);

        // Match integers and decimals
        Pattern pattern = Pattern.compile("\\b\\d+(\\.\\d+)?\\b");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            if (!isInsideStringOrComment(text, matcher.start())) {
                doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), numberStyle, true);
            }
        }
    }

    private boolean isInsideStringOrComment(String text, int position) {
        // Simple check - count quotes before position
        int doubleQuotes = 0;
        int singleQuotes = 0;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        for (int i = 0; i < position && i < text.length(); i++) {
            char c = text.charAt(i);
            char next = (i + 1 < text.length()) ? text.charAt(i + 1) : 0;

            if (inBlockComment) {
                if (c == '*' && next == '/') {
                    inBlockComment = false;
                    i++;
                }
                continue;
            }

            if (inLineComment) {
                if (c == '\n') {
                    inLineComment = false;
                }
                continue;
            }

            if (c == '/' && next == '*') {
                inBlockComment = true;
                i++;
                continue;
            }

            if ((c == '-' && next == '-') || (c == '/' && next == '/')) {
                inLineComment = true;
                i++;
                continue;
            }

            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                doubleQuotes++;
            } else if (c == '\'' && (i == 0 || text.charAt(i - 1) != '\\')) {
                singleQuotes++;
            }
        }

        return (doubleQuotes % 2 == 1) || (singleQuotes % 2 == 1) || inBlockComment || inLineComment;
    }

    /**
     * Creates a JTextPane configured for N1QL syntax highlighting.
     */
    public static JTextPane createHighlightedTextPane() {
        JTextPane textPane = new JTextPane();
        textPane.setEditable(true);
        textPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textPane.setBackground(JBColor.background());
        return textPane;
    }
}
