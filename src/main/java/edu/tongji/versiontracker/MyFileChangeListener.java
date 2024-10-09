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

    @Override
    public void after(@NotNull List<? extends VFileEvent> events) {
        for (VFileEvent event : events) {
            if (event.isFromSave() || event.isFromRefresh()) {
                versionTrackerService.trackChange(event.getFile());
            }
        }
    }
}
