package edu.tongji.versiontracker;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.project.Project;

public class DiffWindow {

    public static void showDiff(Project project, String fileName, String oldContent, String newContent) {
        // 创建 DiffContentFactory 实例
        DiffContentFactory contentFactory = DiffContentFactory.getInstance();

        // 创建旧版本和新版本的 DiffContent
        DiffContent content1 = contentFactory.create(project, oldContent);
        DiffContent content2 = contentFactory.create(project, newContent);

        // 定义标题和内容标题
        String title = "版本差异 - " + fileName;
        String oldTitle = "版本内容";
        String newTitle = "当前内容";

        // 创建 SimpleDiffRequest
        SimpleDiffRequest diffRequest = new SimpleDiffRequest(title, content1, content2, oldTitle, newTitle);

        // 显示差异对话框
        DiffManager.getInstance().showDiff(project, diffRequest);
    }
}
