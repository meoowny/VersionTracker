package edu.tongji.versiontracker;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * 用于监听文件修改的监听器，粒度较细，每次编辑都会触发。
 * 还有一种策略是实现 VirtualFileListener，粒度相对更粗，当文件保存、删除时触发。
 */
public class VersionTrackerListener implements DocumentListener {
    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
        Document document = event.getDocument();
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file != null) {
            var newFragment = event.getNewFragment();
            String filePath = file.getPath();
            System.out.println("File changed: " + filePath);
            System.out.println("Modification: " + newFragment);
            // 关于 IDEA 内置的 Diff 视图（用于呈现文件差异）
            // https://intellij-support.jetbrains.com/hc/en-us/community/posts/115000508990-how-to-invoke-the-built-in-diff-action
            // https://intellij-support.jetbrains.com/hc/en-us/community/posts/206936885-Want-to-use-Diff-View-component
        }
    }
}