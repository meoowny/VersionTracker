package edu.tongji.versiontracker;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class Version {
    private int versionNumber;
    private LocalDateTime timestamp;
    private Map<String, String> snapshots; // 文件名到文件内容的映射

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

    public void addSnapshot(String fileName, String content) {
        snapshots.put(fileName, content);
    }
}
