package edu.tongji.versiontracker;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.EditorFactory;

/**
 * 插件的核心服务类，负责注册和移除插件相关监听器，并且可以维护插件的状态信息。
 * 这里使用轻服务类，因此未在 plugin.xml 中注册。
 */
@Service
public final class VersionTrackerService implements Disposable {
    private final VersionTrackerListener documentListener = new VersionTrackerListener();

    public void initService() {
        System.out.println("Version tracker loaded.");
        // 注册文件修改的监听器
        EditorFactory.getInstance().getEventMulticaster().addDocumentListener(documentListener, this);
    }

    @Override
    public void dispose() {
        // 注销文件修改的监听器
        EditorFactory.getInstance().getEventMulticaster().removeDocumentListener(documentListener);
    }
}
