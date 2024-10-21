package edu.tongji.versiontracker;

import org.jetbrains.annotations.NotNull;

import com.intellij.psi.PsiTreeChangeEvent;
import com.intellij.psi.PsiTreeChangeListener;

/**
 * 文件修改的监听器，用户编辑文件后触发。
 * 粒度较细，修改、创建、删除文件时均会触发。
 * 以下大多空函数为实现 {@link PsiTreeChangeListener} 接口所必要的
 */
public class PsiTreeListener implements PsiTreeChangeListener {
    private final VersionTrackerService service;

    public PsiTreeListener(@NotNull VersionTrackerService service) {
        this.service = service;
    }

    @Override
    public void childAdded(@NotNull PsiTreeChangeEvent event) {
        System.out.println("childAdded");
        testEvent(event);
        service.trackChange(event);
    }

    @Override
    public void childRemoved(@NotNull PsiTreeChangeEvent event) {
        System.out.println("childRemoved");
        testEvent(event);
        service.trackChange(event);
    }

    @Override
    public void childReplaced(@NotNull PsiTreeChangeEvent event) {
        System.out.println("childReplaced");
        testEvent(event);
        service.trackChange(event);
    }

    @Override
    public void childrenChanged(@NotNull PsiTreeChangeEvent event) {
        System.out.println("childrenChanged");
        testEvent(event);
        service.trackChange(event);
    }

    @Override
    public void childMoved(@NotNull PsiTreeChangeEvent event) {
        System.out.println("childMoved");
        testEvent(event);
        service.trackChange(event);
    }

    @Override
    public void beforeChildAddition(@NotNull PsiTreeChangeEvent event) {
    }

    @Override
    public void beforeChildRemoval(@NotNull PsiTreeChangeEvent event) {
    }

    @Override
    public void beforeChildReplacement(@NotNull PsiTreeChangeEvent event) {
    }

    @Override
    public void beforeChildMovement(@NotNull PsiTreeChangeEvent event) {
    }

    @Override
    public void beforeChildrenChange(@NotNull PsiTreeChangeEvent event) {
    }

    @Override
    public void beforePropertyChange(@NotNull PsiTreeChangeEvent event) {
    }

    @Override
    public void propertyChanged(@NotNull PsiTreeChangeEvent event) {
    }

    /**
     * 调试用的函数，将文件修改信息打印出来方便查看
     *
     * @param event
     */
    private void testEvent(@NotNull PsiTreeChangeEvent event) {
        var oldChild = event.getOldChild();
        System.out.println("Property: " + event.getPropertyName());
        if (oldChild != null) {
            System.out.println("Old offset: " + oldChild.getTextOffset());
            System.out.println("Old child: '" + oldChild.getText() + "'");
        }
        var newChild = event.getNewChild();
        if (newChild != null) {
            System.out.println("New offset: " + newChild.getTextOffset());
            System.out.println("New child: '" + newChild.getText() + "'");
        }
        var o = event.getFile();
        if (o != null) {
//            System.out.println("File: " + o.getText());
        }
    }
}
