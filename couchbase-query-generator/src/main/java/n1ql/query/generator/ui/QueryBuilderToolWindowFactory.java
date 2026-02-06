package n1ql.query.generator.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Factory class for creating the N1QL Query Generator tool window.
 */
public class QueryBuilderToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        QueryBuilderPanel queryBuilderPanel = new QueryBuilderPanel(project);
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(queryBuilderPanel.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
