package edu.tongji.versiontracker;

import java.time.LocalDateTime;

public class Increment {
    private final String fileName;
    private final String diffContent; // 文件差异内容（diff 格式）
    private final LocalDateTime timestamp;
    private final String operationType; // 新增、删除、修改

    public Increment(String fileName, String diffContent, String operationType) {
        this.fileName = fileName;
        this.diffContent = diffContent;
        this.operationType = operationType;
        this.timestamp = LocalDateTime.now();
    }

    public String getFileName() {
        return fileName;
    }

    public String getDiffContent() {
        return diffContent;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getOperationType() {
        return operationType;
    }
}
