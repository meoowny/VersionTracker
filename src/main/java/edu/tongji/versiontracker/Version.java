package edu.tongji.versiontracker;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class Version {
    private final int versionNumber;
    private final LocalDateTime timestamp;
    private final Map<String, String> snapshots; // 相对路径到文件内容的映射

    public Version(int versionNumber) {
        this.versionNumber = versionNumber;
        this.timestamp = LocalDateTime.now();
        this.snapshots = new HashMap<>();
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Map<String, String> getSnapshots() {
        return snapshots;
    }

    public void addSnapshot(String relativePath, String content) {
        snapshots.put(relativePath, content);
    }
}
