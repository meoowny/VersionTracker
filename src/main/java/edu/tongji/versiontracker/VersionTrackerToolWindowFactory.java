package edu.tongji.versiontracker;

import java.awt.BorderLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.AbstractTableModel;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

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
        // 创建一个 JLabel 用于显示版本计数
        JLabel versionCountLabel = new JLabel("Total versions: 0");

        // 创建VersionTracker工具窗口内容组件
        VersionTrackerToolWindowContent toolWindowContent = new VersionTrackerToolWindowContent(project, versionCountLabel);

        // 使用ContentFactory创建一个新的Content对象，包含工具窗口内容组件
        Content content = ContentFactory.getInstance().createContent(toolWindowContent.getContent(), "", false);

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
        private final JTable table;
        private final JLabel versionCountLabel;

        /**
         * 构造工具窗口内容
         *
         * @param project           项目对象，用于获取版本跟踪服务
         */
        public VersionTrackerToolWindowContent(Project project, JLabel versionCountLabel) {
            // 初始化版本跟踪服务
            versionTrackerService = project.getService(VersionTrackerService.class);
            this.versionCountLabel = versionCountLabel;
            // 初始化内容面板，使用BorderLayout布局
            contentPanel = new JPanel(new BorderLayout());

            // 创建一个表格，用于显示版本历史数据
            table = new JTable(new VersionHistoryTableModel(versionTrackerService.getVersionHistory()));
            // 将表格放入滚动面板中
            JScrollPane scrollPane = new JScrollPane(table);
            // 将滚动面板添加到内容面板的中心区域
            contentPanel.add(scrollPane, BorderLayout.CENTER);

            // 创建一个刷新按钮
            JButton refreshButton = new JButton("Refresh");
            // 为按钮添加动作监听器，当点击按钮时刷新表格数据
            refreshButton.addActionListener(e -> refreshTable());
            // 将刷新按钮添加到内容面板的南部区域
            contentPanel.add(refreshButton, BorderLayout.SOUTH);

            // 初始化时刷新一次
            refreshTable();
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
         */
        private void refreshTable() {
            List<VersionManager.VersionInfo> history = versionTrackerService.getVersionHistory();
            System.out.println("Refreshing table. Total versions: " + history.size());
            ((VersionHistoryTableModel) table.getModel()).setVersionHistory(history);
            versionCountLabel.setText("Total versions: " + history.size());
        }
    }

    /**
     * 版本历史表模型类，继承自AbstractTableModel
     * 用于在Swing表格中展示版本跟踪服务的历史记录
     */
    private static class VersionHistoryTableModel extends AbstractTableModel {
        private List<VersionManager.VersionInfo> versionHistory;
        private final String[] columnNames = {"Timestamp", "File Path", "Change Type", "View"};

        /**
         * 构造函数，初始化版本历史数据
         * @param versionHistory 版本历史记录列表
         */
        public VersionHistoryTableModel(List<VersionManager.VersionInfo> versionHistory) {
            this.versionHistory = versionHistory;
        }

        /**
         * 设置版本历史数据，并通知观察者数据已更改
         * @param versionHistory 版本历史记录列表
         */
        public void setVersionHistory(List<VersionManager.VersionInfo> versionHistory) {
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
            VersionManager.VersionInfo info = versionHistory.get(rowIndex);
            switch (columnIndex) {
                case 0: return info.timestamp;
                case 1: return info.filePath;
                case 2: return info.changeType;
                case 3: return "View";
                default: return null;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            // 设置单元格是否可编辑
            return columnIndex == 3;
        }

    }
}
