package edu.tongji.versiontracker;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.io.Serializable;

public class Version implements Serializable {
    private static final long serialVersionUID = 1L; // 添加序列化版本号

    private int versionNumber;
    private LocalDateTime timestamp; // 时间戳需要序列化
    private transient Map<String, String> snapshots; // 使用 transient，避免序列化大文件内容

    public Version(int versionNumber) {
        this.versionNumber = versionNumber;
        this.timestamp = LocalDateTime.now();
        this.snapshots = new HashMap<>();
    }

    // Getter 和 Setter 方法

    public int getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, String> getSnapshots() {
        return snapshots;
    }

    public void setSnapshots(Map<String, String> snapshots) {
        this.snapshots = snapshots;
    }

    public void addSnapshot(String relativePath, String content) {
        snapshots.put(relativePath, content);
    }

    // 添加 readObject 和 writeObject 方法，以便在序列化时处理 LocalDateTime
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(timestamp.toString()); // 将 LocalDateTime 转换为字符串保存
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        String timestampStr = (String) in.readObject();
        this.timestamp = LocalDateTime.parse(timestampStr);
        if (this.snapshots == null) {
            this.snapshots = new HashMap<>(); // 反序列化后初始化 snapshots
        }
    }
}
