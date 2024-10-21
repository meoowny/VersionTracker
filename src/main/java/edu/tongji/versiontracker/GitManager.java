package edu.tongji.versiontracker;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;


public class GitManager {
    private final Repository repository;
    private final Git git;

    public GitManager(String projectPath) throws IOException, GitAPIException {
        Path repoPath = Paths.get(projectPath, ".git");
        FileRepositoryBuilder builder = new FileRepositoryBuilder();

        if (!repoPath.toFile().exists()) {
            // 如果仓库不存在，初始化一个新的 Git 仓库
            System.out.println("Initializing new Git repository at: " + projectPath);
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

    public void createBranch(String branchName) throws GitAPIException {
        git.branchCreate().setName(branchName).call();
    }

    public void checkoutBranch(String branchName) throws GitAPIException {
        git.checkout().setName(branchName).call();
    }

    public void commitChanges(String message) throws GitAPIException {
        git.add().addFilepattern(".").call();
        git.commit().setMessage(message).call();
    }

    public void mergeBranch(String sourceBranch) throws GitAPIException, IOException {
        git.checkout().setName("main").call(); // 切换回主分支
        git.merge().include(git.getRepository().findRef(sourceBranch)).call();
    }

    public void close() {
        repository.close();
        git.close();
    }
}
