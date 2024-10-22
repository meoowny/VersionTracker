package edu.tongji.versiontracker;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
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

        // 添加刷新按钮
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadVersionList());

        // 将按钮添加到底部面板
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.add(refreshButton);

        // 添加底部面板到主面板的南边
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);


        // Load version list
        loadVersionList();

        // Subscribe to file selection changes
        project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileSelectionListener(this));
    }


    // Get content
    public JPanel getContent() {
        return mainPanel;
    }

    public void loadVersionList() {
        VirtualFile currentFile = getCurrentFile();
        versionListPanel.removeAll();

        if (currentFile == null) {
            JLabel noFileLabel = new JLabel("No file selected.");
            versionListPanel.add(noFileLabel);
        } else {
            String relativePath = versionManager.getRelativePath(currentFile);
            List<Version> versions = versionManager.getVersionsForFile(relativePath);

            if (versions.isEmpty()) {
                JLabel noVersionsLabel = new JLabel("No versions available for this file.");
                versionListPanel.add(noVersionsLabel);
            } else {
                for (Version version : versions) {
                    VersionItem versionItem = new VersionItem(version);
                    JPanel versionPanel = createVersionPanel(versionItem);
                    versionListPanel.add(versionPanel);
                }
            }
        }

        // Refresh the panel
        versionListPanel.revalidate();
        versionListPanel.repaint();
    }

    private JPanel createVersionPanel(VersionItem versionItem) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60)); // Adjust height

        JLabel versionLabel = new JLabel(versionItem.toString());
        versionLabel.setVerticalAlignment(SwingConstants.TOP);

        JButton diffButton = new JButton("View Diff");
        JButton rollbackButton = new JButton("Rollback");

        diffButton.addActionListener(e -> {
            showDiffDialog(versionItem.getVersion());
        });

        rollbackButton.addActionListener(e -> {
            rollbackToVersion(versionItem.getVersion());
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(diffButton);
        buttonPanel.add(rollbackButton);

        panel.add(versionLabel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.EAST);

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
            return "<html>Version " + version.getVersionNumber() + "<br>" + "<br>" + formattedTimestamp + "</html>";
        }
    }

    // Show diff dialog
    private void showDiffDialog(Version version) {
        VirtualFile currentFile = getCurrentFile();
        if (currentFile == null) {
            JOptionPane.showMessageDialog(mainPanel, "Unable to get the current file.");
            return;
        }

        String currentContent = versionManager.getFileContent(currentFile);

        // 获取要比较的版本内容
        String relativePath = versionManager.getRelativePath(currentFile);
        String versionContent = version.getSnapshots().get(relativePath);

        if (versionContent == null) {
            JOptionPane.showMessageDialog(mainPanel, "There is no record of the current file in this version.");
            return;
        }

        // 使用 IntelliJ 的 Diff 工具显示差异
        showDiffInIdea(project, currentFile, versionContent, currentContent);
    }

    private void showDiffInIdea(Project project, VirtualFile currentFile, String oldContent, String newContent) {
        DiffContentFactory contentFactory = DiffContentFactory.getInstance();
        DiffContent content1 = contentFactory.create(oldContent);
        DiffContent content2 = contentFactory.create(newContent);

        // 假设 SimpleDiffRequest 需要额外的参数或使用不同的构造方法
        SimpleDiffRequest request = new SimpleDiffRequest("Version DIff - " + currentFile.getName(), content1, content2, "Version Content", "Current Content");

        DiffManager.getInstance().showDiff(project, request);
    }

    // Rollback to selected version
    private void rollbackToVersion(Version version) {
        VirtualFile currentFile = getCurrentFile();
        if (currentFile == null) {
            JOptionPane.showMessageDialog(mainPanel, "Unable to get the current file.");
            return;
        }

        // Get the relative path of the current file
        String relativePath = versionManager.getRelativePath(currentFile);
        if (relativePath == null) {
            JOptionPane.showMessageDialog(mainPanel, "Unable to get the relative path of the current file.");
            return;
        }

        String versionContent = version.getSnapshots().get(relativePath);

        if (versionContent == null) {
            JOptionPane.showMessageDialog(mainPanel, "There is no record of the current file in this version.");
            return;
        }

        // Replace the current file content with the version content
        try {
            versionManager.replaceFileContent(currentFile, versionContent);
            JOptionPane.showMessageDialog(mainPanel, "The file has been backported to version " + version.getVersionNumber());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(mainPanel, "Rolling back failed:" + e.getMessage());
            e.printStackTrace();
        }
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
