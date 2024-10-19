package edu.tongji.versiontracker;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import org.jetbrains.annotations.NotNull;

public class ProjectListener implements ProjectManagerListener {
    @Override
    public void projectOpened(@NotNull Project project) {
        VersionTrackerService service = project.getService(VersionTrackerService.class);
    }

    /**
     * 项目关闭时触发，用于退出 IDE 时的版本保存
     *
     * @param project
     */
    @Override
    public void projectClosed(@NotNull Project project) {
        System.out.println("VersionTracker plugin closed for project: " + project.getName());
        // 如果需要在项目关闭时进行清理，可以在这里进行
    }
}
