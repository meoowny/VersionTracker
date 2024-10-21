package edu.tongji.versiontracker;

import java.util.TimerTask;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * 计时器类，可用于定时触发一次保存
 */
public class VersionTrackerTimer extends TimerTask {
    private final Project project;
    private final VersionTrackerService service;

    public VersionTrackerTimer(Project project, VersionTrackerService service) {
        System.out.println("Timer setup...");
        this.project = project;
        this.service = service;
    }

    /**
     * 任务逻辑实现处
     *
     *
     *
     * 10.21刘佳朋修改部分代码，之前代码会出现getProjectFile()得到null，之后调用getChildren()出现空指针异常的错误改为了getBaseDir
     */
    @Override
    public void run() {
        System.out.println("Timer triggered: Saving current version...");
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) {
                return;
            }
            VirtualFile[] openFiles = ProjectManager.getInstance().getOpenProjects()[0].getBaseDir().getChildren();
            for (VirtualFile file : openFiles) {
                if (!file.isDirectory()) {
                    service.trackChange(file);
                }
            }
        });
    }
}
