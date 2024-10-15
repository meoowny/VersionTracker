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

        if (project != null) {
            if (!isGitRepository(project)) {
                initializeGitRepository(project);
            }
            createBranchAndCommitChanges(project);
        }
    }

    /**
     * 创建分支并提交变更到指定项目
     * 此方法演示了如何在给定项目中创建一个新的特性分支，提交一些变更，然后将这些变更合并到主分支
     *
     * @param project 项目对象，包含项目的基本路径等信息
     */
    private void createBranchAndCommitChanges(Project project) {
        try {
            // 获取项目的基本路径
            String basePath = project.getBasePath();
            // 确保项目路径不为空
            if (basePath != null) {
                // 使用插件服务的 git 实例
                Git git = project.getService(VersionTrackerService.class).getGitInstance();

                // 定义新的分支名称
                String newBranchName = "new-feature-branch";
                // 创建并切换到新的分支
                git.branchCreate().setName(newBranchName).call();
                git.checkout().setName(newBranchName).call();

                // 添加所有变更到暂存区
                git.add().addFilepattern(".").call();
                // 提交变更到新的分支
                git.commit().setMessage("Committing changes to new branch").call();

                // 定义主分支名称
                String originalBranch = "main";
                // 切换回主分支
                git.checkout().setName(originalBranch).call();
                // 将新分支的变更合并到主分支
                git.merge().include(git.getRepository().resolve(newBranchName)).call();

                // 输出合并成功的信息
                System.out.println("Changes committed and merged to " + originalBranch);
            }
        } catch (IOException | GitAPIException e) {
            // 处理可能出现的异常
            handleException(project, "Git操作错误：", e);
        }
    }

    /**
     * 初始化Git仓库
     *
     * @param project 项目对象，包含项目的基本信息和操作方法
     *                包括项目的基本路径，用于定位项目根目录
     */
    private void initializeGitRepository(Project project) {
        try {
            // 获取项目的基本路径
            String basePath = project.getBasePath();
            // 检查路径是否为空，防止空指针异常
            if (basePath != null) {
                // 构造Git仓库的目录路径
                File gitDir = new File(basePath, ".git");
                // 检查.git目录是否存在，不存在则初始化
                if (!gitDir.exists()) {
                    // 初始化Git仓库在指定目录下
                    Git.init().setDirectory(new File(basePath)).call();
                    // 输出初始化Git仓库的信息
                    System.out.println("在 " + basePath + " 初始化了新的Git仓库");
                }
            }
        } catch (GitAPIException e) {
            // 处理Git API异常
            handleException(project, "初始化Git仓库错误：", e);
        }
    }

    /**
     * 判断项目是否为Git仓库
     * 通过检查项目根目录下是否存在.git目录来确定
     *
     * @param project 项目对象，包含项目的基本信息
     * @return 如果项目是Git仓库，则返回true；否则返回false
     */
    private boolean isGitRepository(Project project) {
        // 获取项目的根目录路径
        String basePath = project.getBasePath();
        // 如果根目录路径为空，则直接返回false
        if (basePath != null) {
            // 构造一个File对象，指向项目根目录下的.git目录
            File gitDir = new File(basePath, ".git");
            // 检查.git目录是否存在且为目录，如果是则返回true，表示该项目是Git仓库
            return gitDir.exists() && gitDir.isDirectory();
        }
        // 如果根目录路径为空，或者.git目录不存在，则返回false
        return false;
    }

    private void handleException(Project project, String message, Exception e) {
        String errorMessage = message + e.getMessage();
        System.err.println(errorMessage);
        Messages.showErrorDialog(project, errorMessage, "错误");
    }

    // 如果需要处理网络请求，可以添加以下方法
    private String handleRedirect(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(false);
        int status = conn.getResponseCode();
        if (status == HttpURLConnection.HTTP_MOVED_TEMP
            || status == HttpURLConnection.HTTP_MOVED_PERM
            || status == HttpURLConnection.HTTP_SEE_OTHER) {
            String newUrl = conn.getHeaderField("Location");
            if (newUrl == null) {
                throw new IOException("重定向URL为空");
            }
            return newUrl;
        }
        return urlString;
    }
}
