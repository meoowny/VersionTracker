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
    
    private void createBranchAndCommitChanges(Project project) {
        try {
            String basePath = project.getBasePath();
            if (basePath != null) {
                Git git = Git.open(new File(basePath));
                
                String newBranchName = "new-feature-branch";
                git.branchCreate().setName(newBranchName).call();
                git.checkout().setName(newBranchName).call();
                
                git.add().addFilepattern(".").call();
                git.commit().setMessage("Committing changes to new branch").call();
                
                String originalBranch = "main";
                git.checkout().setName(originalBranch).call();
                git.merge().include(git.getRepository().resolve(newBranchName)).call();
                
                System.out.println("Changes committed and merged to " + originalBranch);
            }
        } catch (IOException | GitAPIException e) {
            handleException(project, "Git操作错误：", e);
        }
    }
    
    private void initializeGitRepository(Project project) {
        try {
            String basePath = project.getBasePath();
            if (basePath != null) {
                File gitDir = new File(basePath, ".git");
                if (!gitDir.exists()) {
                    Git.init().setDirectory(new File(basePath)).call();
                    System.out.println("在 " + basePath + " 初始化了新的Git仓库");
                }
            }
        } catch (GitAPIException e) {
            handleException(project, "初始化Git仓库错误：", e);
        }
    }
    
    private boolean isGitRepository(Project project) {
        String basePath = project.getBasePath();
        if (basePath != null) {
            File gitDir = new File(basePath, ".git");
            return gitDir.exists() && gitDir.isDirectory();
        }
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
