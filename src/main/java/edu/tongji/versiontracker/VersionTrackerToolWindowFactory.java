package edu.tongji.versiontracker;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * 用户界面的工厂类，可用于构建查询修改记录的界面，基于 IDEA 的 Tool Window 界面实现。
 */
public class VersionTrackerToolWindowFactory implements ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        Content content = ContentFactory.getInstance().createContent(new VersionTrackerToolWindow().getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
