package edu.tongji.versiontracker;

import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 文件修改监听器，用于监听文件创建、保存与删除。
 * 粒度较粗，用户手动保存后触发
 */
public class FileListener implements BulkFileListener {
    private final Project project;
    private final VersionTrackerService versionTrackerService;

    public FileListener(Project project, VersionTrackerService service) {
        this.project = project;
        this.versionTrackerService = service;
    }

    /**
     * 在文件事件处理后执行操作
     * 该方法主要用于处理一组文件事件，包括文件的保存和刷新
     * 通过调用版本跟踪服务来跟踪这些事件引起的文件变化
     * 可以做一次全量保存
     *
     * @param events 不可为空的文件事件列表这些事件可以是文件保存或刷新等操作
     */
    @Override
    public void after(@NotNull List<? extends VFileEvent> events) {
        System.out.println("after");
        for (VFileEvent event : events) {
            // 检查事件是否由文件保存或刷新触发，如果是，则跟踪文件的变化
            if (event.isFromSave() || event.isFromRefresh()) {
                versionTrackerService.trackChange(event.getFile());
            }
        }
    }
}
