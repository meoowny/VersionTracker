package edu.tongji.versiontracker;

import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import org.jetbrains.annotations.NotNull;

public class FileSelectionListener implements FileEditorManagerListener {

    private final VersionTrackerToolWindow toolWindow;

    public FileSelectionListener(VersionTrackerToolWindow toolWindow) {
        this.toolWindow = toolWindow;
    }

    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        toolWindow.loadVersionList(); // Update the version list when the selection changes
    }
}
