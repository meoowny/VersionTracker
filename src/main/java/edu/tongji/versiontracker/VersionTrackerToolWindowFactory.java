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
    /**
     * 创建工具窗口内容
     * 该方法用于在工具窗口中添加特定的内容组件，这里是VersionTracker工具窗口内容组件
     * 它通过创建VersionTrackerToolWindowContent实例，并将其添加到工具窗口中，实现了工具窗口的功能展示
     *
     * @param project 非空的Project对象，表示当前项目
     * @param toolWindow 非空的ToolWindow对象，表示当前工具窗口
     */
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // 创建VersionTracker工具窗口内容组件
        VersionTrackerToolWindowContent toolWindowContent = new VersionTrackerToolWindowContent(project);

        // 使用ContentFactory创建一个新的Content对象，包含工具窗口内容组件
        Content content = ContentFactory.SERVICE.getInstance().createContent(toolWindowContent.getContent(), "", false);

        // 将新创建的内容组件添加到工具窗口的内容管理器中
        toolWindow.getContentManager().addContent(content);
    }

    /**
     * 版本跟踪工具窗口内容类
     * 该类负责创建和管理工具窗口中的内容，包括一个显示版本历史的表格和一个刷新按钮
     */
    private static class VersionTrackerToolWindowContent {
        // 工具窗口的内容面板
        private final JPanel contentPanel;
        // 版本跟踪服务，用于获取版本历史
        private final VersionTrackerService versionTrackerService;

        /**
         * 构造工具窗口内容
         *
         * @param project 项目对象，用于获取版本跟踪服务
         */
        public VersionTrackerToolWindowContent(Project project) {
            // 初始化版本跟踪服务
            versionTrackerService = project.getService(VersionTrackerService.class);
            // 初始化内容面板，使用BorderLayout布局
            contentPanel = new JPanel(new BorderLayout());

            // 创建一个表格，用于显示版本历史数据
            JTable table = new JTable(new VersionHistoryTableModel(versionTrackerService.getVersionHistory()));
            // 将表格放入滚动面板中
            JScrollPane scrollPane = new JScrollPane(table);
            // 将滚动面板添加到内容面板的中心区域
            contentPanel.add(scrollPane, BorderLayout.CENTER);

            // 创建一个刷新按钮
            JButton refreshButton = new JButton("Refresh");
            // 为按钮添加动作监听器，当点击按钮时刷新表格数据
            refreshButton.addActionListener(e -> refreshTable(table));
            // 将刷新按钮添加到内容面板的南部区域
            contentPanel.add(refreshButton, BorderLayout.SOUTH);
        }

        /**
         * 获取内容面板
         *
         * @return 返回内容面板对象
         */
        public JPanel getContent() {
            return contentPanel;
        }

        /**
         * 刷新表格数据
         *
         * @param table 要刷新的表格
         */
        private void refreshTable(JTable table) {
            // 将版本历史数据设置到表格模型中
            ((VersionHistoryTableModel) table.getModel()).setVersionHistory(versionTrackerService.getVersionHistory());
        }
    }

    /**
     * 版本历史表模型类，继承自AbstractTableModel
     * 用于在Swing表格中展示版本跟踪服务的历史记录
     */
    private static class VersionHistoryTableModel extends AbstractTableModel {
        private List<VersionTrackerService.VersionInfo> versionHistory;
        private final String[] columnNames = {"Timestamp", "File Path", "View"};

        /**
         * 构造函数，初始化版本历史数据
         * @param versionHistory 版本历史记录列表
         */
        public VersionHistoryTableModel(List<VersionTrackerService.VersionInfo> versionHistory) {
            this.versionHistory = versionHistory;
        }

        /**
         * 设置版本历史数据，并通知观察者数据已更改
         * @param versionHistory 版本历史记录列表
         */
        public void setVersionHistory(List<VersionTrackerService.VersionInfo> versionHistory) {
            this.versionHistory = versionHistory;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            // 返回行数，即版本历史记录的数量
            return versionHistory.size();
        }

        @Override
        public int getColumnCount() {
            // 返回列数
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            // 返回指定列的列名
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            // 获取指定单元格的值
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
            // 设置单元格是否可编辑
            return columnIndex == 2;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            // 单元格值设置操作，用于查看版本内容
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
