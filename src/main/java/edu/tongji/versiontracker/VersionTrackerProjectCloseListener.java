package edu.tongji.versiontracker;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCloseListener;
import org.jetbrains.annotations.NotNull;

public class VersionTrackerProjectCloseListener implements ProjectCloseListener {

    @Override
    public void projectClosing(@NotNull Project project) {
        VersionManager versionManager = new VersionManager(project);
        versionManager.saveAllOpenFilesVersion();
    }
}
