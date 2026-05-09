package com.genersoft.iot.vmp.dahua.bean;

import lombok.Data;
import java.io.RandomAccessFile;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 录像文件下载任务
 */
@Data
public class DownloadTask {
    
    private String taskId;              // 任务ID
    private String fileId;              // 文件ID（设备端）
    private String deviceId;            // 设备ID
    private int channelId;              // 通道ID
    
    // 文件信息
    private String fileName;            // 文件名
    private long fileSize;              // 文件大小（字节）
    private long downloadedSize;        // 已下载大小
    private String fileType;            // 文件类型 (MP4/DAV等)
    
    // 时间范围
    private long startTime;             // 录像开始时间
    private long endTime;               // 录像结束时间
    
    // 下载状态
    private volatile boolean downloading;   // 是否正在下载
    private volatile boolean completed;     // 是否完成
    private volatile boolean failed;        // 是否失败
    private String errorMessage;            // 错误信息
    
    // HTTP Range支持
    private long rangeStart;            // 请求起始位置
    private long rangeEnd;              // 请求结束位置
    
    // 进度统计
    private AtomicLong bytesTransferred;    // 已传输字节数
    private long transferStartTime;         // 传输开始时间
    private long lastTransferTime;          // 最后传输时间
    
    // 本地临时文件（用于转码或缓存）
    private String tempFilePath;
    private RandomAccessFile tempFile;
    
    public DownloadTask() {
        this.bytesTransferred = new AtomicLong(0);
        this.downloading = false;
        this.completed = false;
        this.failed = false;
        this.transferStartTime = System.currentTimeMillis();
        this.lastTransferTime = System.currentTimeMillis();
    }
    
    /**
     * 获取下载进度百分比
     */
    public double getProgress() {
        if (fileSize <= 0) {
            return 0.0;
        }
        return (double) downloadedSize / fileSize * 100.0;
    }
    
    /**
     * 获取下载速度（字节/秒）
     */
    public long getTransferSpeed() {
        long currentTime = System.currentTimeMillis();
        long timeDiff = (currentTime - transferStartTime) / 1000;
        if (timeDiff <= 0) {
            return 0;
        }
        return bytesTransferred.get() / timeDiff;
    }
    
    /**
     * 更新传输统计
     */
    public void updateTransferStats(long bytes) {
        bytesTransferred.addAndGet(bytes);
        downloadedSize += bytes;
        lastTransferTime = System.currentTimeMillis();
    }
    
    /**
     * 检查是否超时（5分钟无数据传输）
     */
    public boolean isTimeout() {
        return System.currentTimeMillis() - lastTransferTime > 5 * 60 * 1000;
    }
}
