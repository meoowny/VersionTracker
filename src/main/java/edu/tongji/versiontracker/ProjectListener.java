package edu.tongji.versiontracker;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCloseListener;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.project.ProjectManagerListener;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VirtualFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

public class ProjectListener implements StartupActivity.DumbAware, ProjectManagerListener {
    @Override
    public void runActivity(@NotNull Project project) {
        VersionTrackerService service = project.getService(VersionTrackerService.class);
        if (service != null) {
            ApplicationManager.getApplication().runWriteAction(() -> {
                try {
                    Path versionDirectory = Paths.get(project.getBasePath(), ".version_tracker");
                    Files.createDirectories(versionDirectory);
                    System.out.println("Version directory created at: " + versionDirectory);
                } catch (IOException e) {
                    System.err.println("Failed to create version directory: " + e.getMessage());
                }
            });
        }
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
