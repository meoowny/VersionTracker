package edu.tongji.versiontracker;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;
import java.time.Duration;
import java.time.LocalDateTime;

public class VersionTrackerToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, ToolWindow toolWindow) {
        VersionTrackerToolWindow versionTrackerToolWindow = new VersionTrackerToolWindow(project);
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(versionTrackerToolWindow.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }
}