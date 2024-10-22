package edu.tongji.versiontracker;

import com.intellij.openapi.diagnostic.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;


public class GitManager {
    private static final Logger LOG = Logger.getInstance(VersionManager.class);

    private final Repository repository;
    private final Git git;

    public GitManager(String projectPath) throws IOException, GitAPIException {
        Path repoPath = Paths.get(projectPath, ".git");
        FileRepositoryBuilder builder = new FileRepositoryBuilder();

        if (!repoPath.toFile().exists()) {
            // 如果仓库不存在，初始化一个新的 Git 仓库
            LOG.info("Initializing new Git repository at: " + projectPath);
            this.git = Git.init().setDirectory(new File(projectPath)).call();
            this.repository = git.getRepository();
        } else {
            this.repository = builder.setGitDir(repoPath.toFile())
                .readEnvironment()
                .findGitDir()
                .build();
            this.git = new Git(repository);
        }
    }

    // 创建分支，如已有分支则仅打印提示
    public void createBranch(String branchName) throws GitAPIException {
        try {
            git.branchCreate().setName(branchName).call();
        } catch (GitAPIException e) {
            if (e.getMessage().contains("already exists")) {
                LOG.info("Branch " + branchName + " already exists");
            } else {
                throw e;
            }
        }
    }

    // 创建并切换分支
    public void checkoutBranch(String branchName) throws GitAPIException {
        createBranch(branchName);
        git.checkout().setName(branchName).call();
    }

    public void commitChanges(String message) throws GitAPIException {
        git.add().addFilepattern(".").call();
        git.commit().setMessage(message).call();
    }

    // 合并分支并删除被合并的分支
    public void mergeBranch(String sourceBranch, String currentBranch) throws GitAPIException, IOException {
        git.checkout().setName(currentBranch).call(); // 切换回原分支
        // 压缩合并提交
        git.merge().setSquash(true).include(git.getRepository().findRef(sourceBranch)).call();
        commitChanges("New track");

        git.branchDelete().setForce(true).setBranchNames(sourceBranch).call();
    }

    public void close() {
        repository.close();
        git.close();
    }

    public String getCurrentBranch() throws IOException {
        return repository.getBranch();
    }
}
