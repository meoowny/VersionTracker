package edu.tongji.versiontracker;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

/**
 * 验证用的 Action，注册到 Help 下，点击后显示当前项目标题
 * 似乎被绑定了 git 的相关逻辑
 */
public class ShowProjectNameAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        String name = project != null ? project.getName() : "";
        Messages.showMessageDialog("项目名称为：" + name, "标题", Messages.getInformationIcon());
    }
}
