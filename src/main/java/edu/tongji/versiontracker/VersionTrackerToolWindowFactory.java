package edu.tongji.versiontracker;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class VersionTrackerToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        VersionTrackerToolWindowContent toolWindowContent = new VersionTrackerToolWindowContent(project);
        Content content = ContentFactory.SERVICE.getInstance().createContent(toolWindowContent.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }

    private static class VersionTrackerToolWindowContent {
        private final JPanel contentPanel;
        private final VersionTrackerService versionTrackerService;

        public VersionTrackerToolWindowContent(Project project) {
            versionTrackerService = project.getService(VersionTrackerService.class);
            contentPanel = new JPanel(new BorderLayout());

            JTable table = new JTable(new VersionHistoryTableModel(versionTrackerService.getVersionHistory()));
            JScrollPane scrollPane = new JScrollPane(table);
            contentPanel.add(scrollPane, BorderLayout.CENTER);

            JButton refreshButton = new JButton("Refresh");
            refreshButton.addActionListener(e -> refreshTable(table));
            contentPanel.add(refreshButton, BorderLayout.SOUTH);
        }

        public JPanel getContent() {
            return contentPanel;
        }

        private void refreshTable(JTable table) {
            ((VersionHistoryTableModel) table.getModel()).setVersionHistory(versionTrackerService.getVersionHistory());
        }
    }

    private static class VersionHistoryTableModel extends AbstractTableModel {
        private List<VersionTrackerService.VersionInfo> versionHistory;
        private final String[] columnNames = {"Timestamp", "File Path", "View"};

        public VersionHistoryTableModel(List<VersionTrackerService.VersionInfo> versionHistory) {
            this.versionHistory = versionHistory;
        }

        public void setVersionHistory(List<VersionTrackerService.VersionInfo> versionHistory) {
            this.versionHistory = versionHistory;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return versionHistory.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            VersionTrackerService.VersionInfo info = versionHistory.get(rowIndex);
            switch (columnIndex) {
                case 0: return info.timestamp;
                case 1: return info.filePath;
                case 2: return "View";
                default: return null;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 2;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == 2) {
                VersionTrackerService.VersionInfo info = versionHistory.get(rowIndex);
                try {
                    String content = new String(Files.readAllBytes(Paths.get(info.versionFilePath)));
                    JTextArea textArea = new JTextArea(content);
                    textArea.setEditable(false);
                    JScrollPane scrollPane = new JScrollPane(textArea);
                    JOptionPane.showMessageDialog(null, scrollPane, "Version Content", JOptionPane.PLAIN_MESSAGE);
                } catch (IOException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Error reading version file", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
}
