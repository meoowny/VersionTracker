package edu.tongji.versiontracker;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class VersionTrackerToolWindow {

    private final JPanel mainPanel;
    private final Project project;
    private final VersionManager versionManager;
    private final JPanel versionListPanel;

    public VersionTrackerToolWindow(Project project) {
        this.project = project;
        this.versionManager = new VersionManager(project);
        this.mainPanel = new JPanel(new BorderLayout());

        // Initialize version list panel
        versionListPanel = new JPanel();
        versionListPanel.setLayout(new BoxLayout(versionListPanel, BoxLayout.Y_AXIS));

        // Add version list panel to scroll pane
        JScrollPane scrollPane = new JScrollPane(versionListPanel);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Load version list
        loadVersionList();
    }

    // Get content
    public JPanel getContent() {
        return mainPanel;
    }

    private void loadVersionList() {
        List<Version> versions = versionManager.getAllVersions();
        versionListPanel.removeAll();

        for (Version version : versions) {
            VersionItem versionItem = new VersionItem(version);
            JPanel versionPanel = createVersionPanel(versionItem);
            versionListPanel.add(versionPanel);
        }

        // Refresh the panel
        versionListPanel.revalidate();
        versionListPanel.repaint();
    }

    private JPanel createVersionPanel(VersionItem versionItem) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40)); // Increase width

        JLabel versionLabel = new JLabel(versionItem.toString());
        JButton rollbackButton = new JButton("回溯");

        rollbackButton.addActionListener(e -> {
            showDiffDialog(versionItem.getVersion());
        });

        panel.add(versionLabel, BorderLayout.CENTER);
        panel.add(rollbackButton, BorderLayout.EAST);

        // Add border for better visibility (optional)
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        return panel;
    }

    // VersionItem class with timestamp formatting
    private static class VersionItem {
        private final Version version;

        public VersionItem(Version version) {
            this.version = version;
        }

        public Version getVersion() {
            return version;
        }

        @Override
        public String toString() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedTimestamp = version.getTimestamp().format(formatter);
            return "Version " + version.getVersionNumber() + " - " + formattedTimestamp;
        }
    }

    // Show diff dialog
    private void showDiffDialog(Version version) {
        VirtualFile currentFile = getCurrentFile();
        if (currentFile == null) {
            JOptionPane.showMessageDialog(mainPanel, "无法获取当前文件。");
            return;
        }

        String currentContent = versionManager.getFileContent(currentFile);
        String versionContent = version.getSnapshots().get(currentFile.getName());

        if (versionContent == null) {
            JOptionPane.showMessageDialog(mainPanel, "该版本中没有当前文件的记录。");
            return;
        }

        // Use Diff tool to show difference
        DiffWindow.showDiff(project, currentFile.getName(), versionContent, currentContent);
    }

    // Get current file
    private VirtualFile getCurrentFile() {
        VirtualFile[] files = FileEditorManager.getInstance(project).getSelectedFiles();
        if (files.length > 0) {
            return files[0];
        } else {
            return null;
        }
    }
}
