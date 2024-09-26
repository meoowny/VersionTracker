package edu.tongji.versiontracker;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 一个测试用途的文件修改监听器，最终代码中应当移除。
 */
public class MyFileChangeListener implements BulkFileListener {
    @Override
    public void after(@NotNull List<? extends VFileEvent> events) {
        StringBuilder content = new StringBuilder();
        for (VFileEvent event : events)
            if (event instanceof VFileContentChangeEvent) {
                var file = event.getFile();
                content.append(file.getExtension()).append("\n");
            }

        var note = new Notification("VersionTrackerGroup", "Test for notification\n" + content, NotificationType.INFORMATION);
        Notifications.Bus.notify(note);
    }
}
