package edu.tongji.versiontracker;

import javax.swing.*;

/**
 * 修改记录查询界面，具体界面绘制见同名 form 文件。
 * 打算参考腾讯文档的修改记录界面，在这里逐条列出版本信息，双击后触发对应的动作查看版本间差异。
 * 关于差异展示界面的说明，见 {@link VersionTrackerListener} 的注释
 */
public class VersionTrackerToolWindow {
    private JPanel panel;

    public JPanel getContent() {
        return panel;
    }
}
