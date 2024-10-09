package edu.tongji.versiontracker;


import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import org.jetbrains.annotations.NotNull;

final class MyToolWindowListener implements ToolWindowManagerListener {
    @Override
    public void stateChanged(@NotNull ToolWindowManager toolWindowManager, @NotNull ToolWindowManagerListener.ToolWindowManagerEventType changeType) {
        if (changeType.equals(ToolWindowManagerEventType.RegisterToolWindow)) {
            Messages.showMessageDialog("Triggered by " + changeType.name(), "Title", Messages.getInformationIcon());
        }
    }
}
