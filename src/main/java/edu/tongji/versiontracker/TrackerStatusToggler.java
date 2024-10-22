package edu.tongji.versiontracker;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

public class TrackerStatusToggler extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        VersionTrackerService service = project.getService(VersionTrackerService.class);
        if (service == null) {
            return;
        }
        service.toggleTrackerStatus();
    }
}
