package edu.tongji.versiontracker;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;

import java.util.TimerTask;

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
     */
    @Override
    public void run() {
        System.out.println("test");
        // TODO: 调用相关接口做一次保存
    }
}
