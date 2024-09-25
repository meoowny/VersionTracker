package edu.tongji.versiontracker;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

public class ShowProjectNameAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        String name = project != null ? project.getName() : "";
        Messages.showMessageDialog("项目名称为：" + name, "标题", Messages.getInformationIcon());
    }
}
