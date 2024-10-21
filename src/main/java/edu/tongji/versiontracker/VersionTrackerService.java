package edu.tongji.versiontracker;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.eclipse.jgit.api.Git;
import org.jetbrains.annotations.NotNull;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiTreeChangeEvent;

/**
 * 插件的核心服务类，负责注册和移除插件部分相关监听器，并且可以维护插件的状态信息。
 */
public final class VersionTrackerService implements Disposable {
    private final Project project;
    private Timer timer = new Timer();
    private final VersionManager versionManager;
    private final GitManager gitManager;
    private String fineGrainedBranchName;

    public VersionTrackerService(Project project) throws IOException, GitAPIException {
        System.out.println("VersionTrackerService init...");

        if (project == null || project.getBasePath() == null) {
            throw new IllegalArgumentException("Invalid project: Project is null or base path is not available");
        }

        this.project = project;
        this.versionManager = new VersionManager(project);

        // 创建版本目录，确保路径有效
        versionManager.createVersionDirectory();

        startTimer();

        // 初始化 GitManager，如果项目没有 Git 仓库，则初始化一个新的
        this.gitManager = new GitManager(project.getBasePath());

        // 注册文件修改的监听器
        PsiManager psiManager = PsiManager.getInstance(project);
        if (psiManager != null) {
            psiManager.addPsiTreeChangeListener(new PsiTreeListener(this), this);
        } else {
            System.err.println("Failed to initialize PsiManager for the project");
        }

        // 注册 VFS 变更监听器
        project.getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES, new FileListener(project, this));
    }

    // 用户点击提交按钮时调用
    public void onGitCommitButtonClick() throws GitAPIException {
        // 自动创建细粒度修改分支
        fineGrainedBranchName = "fine-grained-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        gitManager.createBranch(fineGrainedBranchName);
        gitManager.checkoutBranch(fineGrainedBranchName);
        System.out.println("Created and switched to fine-grained branch: " + fineGrainedBranchName);
    }

    // 每次增量保存时调用，作为单独的 commit 提交到细粒度修改分支
    public void saveFineGrainedVersion(String changeDescription) {
        if (fineGrainedBranchName == null) {
            System.err.println("Fine-grained branch has not been created yet.");
            return;
        }
        try {
            gitManager.commitChanges(changeDescription);
            System.out.println("Committed fine-grained change to branch: " + fineGrainedBranchName);
        } catch (GitAPIException e) {
            System.err.println("Failed to commit fine-grained change: " + e.getMessage());
        }
    }

    // 当细粒度修改达到一定数量或新版本创建时调用
    public void mergeFineGrainedChanges(String versionDescription) {
        try {
            // 将细粒度修改合并为单独的 commit，并提交到主分支
            gitManager.checkoutBranch("main");
            gitManager.mergeBranch(fineGrainedBranchName);
            gitManager.commitChanges(versionDescription);
            System.out.println("Merged fine-grained changes into main branch with message: " + versionDescription);
        } catch (GitAPIException e) {
            System.err.println("Failed to merge fine-grained changes: " + e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void dispose() {
        System.out.println("VersionTrackerService dispose...");

        // 停止定时器
        stopTimer();

        // 移除所有监听器
        PsiManager.getInstance(project).removePsiTreeChangeListener(new PsiTreeListener(this));
        project.getMessageBus().connect().disconnect();

        // 关闭 Git 实例（如果有的话）
        gitManager.close();

        // // 清理版本管理器
        // if (versionManager != null) {
        //     versionManager.cleanup();
        // }

        System.out.println("VersionTrackerService disposed successfully.");
    }

    public void startTimer() {
        if (timer != null) {
            stopTimer();
        }
        timer = new Timer();

        // 设置定时器每60秒触发一次
        timer.schedule(new VersionTrackerTimer(project, this), 0, 60000);
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
