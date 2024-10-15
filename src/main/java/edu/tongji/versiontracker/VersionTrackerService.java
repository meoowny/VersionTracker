package edu.tongji.versiontracker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileAdapter;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiTreeChangeEvent;
import org.eclipse.jgit.api.Git;
import org.jetbrains.annotations.NotNull;

/**
 * 插件的核心服务类，负责注册和移除插件部分相关监听器，并且可以维护插件的状态信息。
 */
public final class VersionTrackerService implements Disposable {
    private final Project project;
    private Timer timer = new Timer();
    private final VersionManager versionManager;

    public VersionTrackerService(Project project) throws IOException {
        System.out.println("VersionTrackerService init...");
        this.project = project;
        this.versionManager = new VersionManager(project);
        versionManager.createVersionDirectory();

        // TODO: 定时器触发逻辑待调整
        startTimer();

        // 注册文件修改的监听器
        PsiManager.getInstance(project).addPsiTreeChangeListener(new PsiTreeListener(this), this);
        project.getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES, new FileListener(project, this));
    }

    public Git getGitInstance() {
        return this.versionManager.git;
    }

    @Override
    public void dispose() {
        System.out.println("VersionTrackerService dispose...");
        // TODO: 服务注销时需要执行的任务
    }

    public void startTimer() {
        if (timer != null) {
            stopTimer();
        }
        timer = new Timer();

        // TODO: 定时器触发逻辑待调整
        timer.schedule(new VersionTrackerTimer(project, this), 2000, 2000);
    }

    public void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    /**
     * 跟踪文件变更
     * 该方法用于跟踪给定文件的变更，只跟踪有效且非空的文件
     * 如果文件为空、无效或为目录，将不执行任何操作
     *
     * @param file 要跟踪的文件，不能为空且必须是有效的非目录文件
     */
    public void trackChange(VirtualFile file) {
        versionManager.trackChange(file);
    }

    public void trackChange(@NotNull PsiTreeChangeEvent event) {
        versionManager.trackChange(event);
    }

    /**
     * 获取版本历史记录
     *
     * @return 返回一个包含所有版本信息的列表
     */
    public List<VersionManager.VersionInfo> getVersionHistory() {
        return versionManager.getVersionHistory();
    }
}
