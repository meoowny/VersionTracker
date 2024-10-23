package edu.tongji.versiontracker;

import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiManager;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

public final class VersionTrackerService {

    private final Project project;
    private final VersionManager versionManager;

    public VersionTrackerService(Project project) {
        this.project = project;
        this.versionManager = new VersionManager(project);
        registerListeners(versionManager);
        versionManager.initialize();
    }

    void registerListeners(VersionManager versionManager) {
        // 注册 FileListener
        MessageBusConnection connection = project.getMessageBus().connect();
        connection.subscribe(VirtualFileManager.VFS_CHANGES, new FileSaveListener(this.versionManager));

        // 注册 FileOpenListener
        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileOpenListener(versionManager));

        // 注册 FileCloseListener
        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileCloseListener((this.versionManager), project));

        // 注册 PsiTreeListener
        PsiManager.getInstance(project).addPsiTreeChangeListener(new PsiTreeListener(this.versionManager), project);

        // 使用 ProjectManager 添加项目关闭监听器
        ProjectManager.getInstance().addProjectManagerListener(project, new ProjectManagerListener() {
            @Override
            public void projectClosingBeforeSave(@NotNull Project project) {
                versionManager.saveAllOpenFilesVersion();
            }
        });
    }

    public void toggleTrackerStatus() {
        versionManager.toggleTrackerStatus();
    }
}
