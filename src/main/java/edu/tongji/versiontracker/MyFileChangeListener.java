package edu.tongji.versiontracker;

import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 一个测试用途的文件修改监听器，最终代码中应当移除。
 */
public class MyFileChangeListener implements BulkFileListener {
    private final Project project;
    private final VersionTrackerService versionTrackerService;

    public MyFileChangeListener(Project project) {
        this.project = project;
        this.versionTrackerService = project.getService(VersionTrackerService.class);
    }

    /**
     * 在文件事件处理后执行操作
     * 该方法主要用于处理一组文件事件，包括文件的保存和刷新
     * 通过调用版本跟踪服务来跟踪这些事件引起的文件变化
     *
     * @param events 不可为空的文件事件列表这些事件可以是文件保存或刷新等操作
     */
    @Override
    public void after(@NotNull List<? extends VFileEvent> events) {
        for (VFileEvent event : events) {
            // 检查事件是否由文件保存或刷新触发，如果是，则跟踪文件的变化
            if (event.isFromSave() || event.isFromRefresh()) {
                versionTrackerService.trackChange(event.getFile());
            }
        }
    }
}
