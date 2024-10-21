package edu.tongji.versiontracker;

import com.intellij.psi.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import groovyjarjarantlr4.v4.runtime.misc.NotNull;

/**
 * 文件修改的监听器，用户编辑文件后触发。
 * 粒度较细，修改、创建、删除文件时均会触发。
 * 以下大多空函数为实现 {@link PsiTreeChangeListener} 接口所必要的
 */
public class PsiTreeListener implements PsiTreeChangeListener {

    private final VersionManager versionManager;

    public PsiTreeListener(VersionManager versionManager) {
        this.versionManager = versionManager;
    }

    @Override
    public void beforeChildAddition(@org.jetbrains.annotations.NotNull PsiTreeChangeEvent event) {

    }

    @Override
    public void beforeChildRemoval(@org.jetbrains.annotations.NotNull PsiTreeChangeEvent event) {

    }

    @Override
    public void beforeChildReplacement(@org.jetbrains.annotations.NotNull PsiTreeChangeEvent event) {

    }

    @Override
    public void beforeChildMovement(@org.jetbrains.annotations.NotNull PsiTreeChangeEvent event) {

    }

    @Override
    public void beforeChildrenChange(@org.jetbrains.annotations.NotNull PsiTreeChangeEvent event) {

    }

    @Override
    public void beforePropertyChange(@org.jetbrains.annotations.NotNull PsiTreeChangeEvent event) {

    }

    @Override
    public void childAdded(@NotNull PsiTreeChangeEvent event) {
        handleEvent(event);
    }

    @Override
    public void childRemoved(@NotNull PsiTreeChangeEvent event) {
        handleEvent(event);
    }

    @Override
    public void childReplaced(@NotNull PsiTreeChangeEvent event) {
        handleEvent(event);
    }

    @Override
    public void childrenChanged(@NotNull PsiTreeChangeEvent event) {
        handleEvent(event);
    }

    @Override
    public void childMoved(@NotNull PsiTreeChangeEvent event) {
        handleEvent(event);
    }

    @Override
    public void propertyChanged(@NotNull PsiTreeChangeEvent event) {
        handleEvent(event);
    }

    private void handleEvent(PsiTreeChangeEvent event) {
        PsiFile psiFile = event.getFile();
        if (psiFile != null) {
            VirtualFile file = psiFile.getVirtualFile();
            if (file != null && !file.isDirectory()) {
                versionManager.handlePsiEvent(file);
            }
        }
    }

}
