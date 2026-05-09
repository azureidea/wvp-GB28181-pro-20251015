package com.genersoft.iot.vmp.dahua.download;

import com.genersoft.iot.vmp.dahua.bean.DownloadTask;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.stream.ChunkedFile;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 录像文件下载代理
 * 支持HTTP Range请求，实现断点续传和流式传输
 */
@Slf4j
public class DahuaDownloadProxy {
    
    private final ConcurrentHashMap<String, DownloadTask> activeTasks;
    private final ExecutorService downloadExecutor;
    private final int maxConcurrentDownloads;
    
    public DahuaDownloadProxy(int maxConcurrentDownloads) {
        this.activeTasks = new ConcurrentHashMap<>();
        this.maxConcurrentDownloads = maxConcurrentDownloads;
        this.downloadExecutor = Executors.newFixedThreadPool(
            maxConcurrentDownloads, 
            r -> {
                Thread t = new Thread(r, "DahuaDownloadWorker");
                t.setDaemon(true);
                return t;
            }
        );
    }
    
    /**
     * 创建下载任务
     */
    public DownloadTask createDownloadTask(String deviceId, int channelId, 
                                            String fileId, String fileName,
                                            long fileSize, long startTime, long endTime) {
        if (activeTasks.size() >= maxConcurrentDownloads) {
            log.warn("Maximum concurrent downloads reached: {}", maxConcurrentDownloads);
            return null;
        }
        
        DownloadTask task = new DownloadTask();
        task.setTaskId(generateTaskId(deviceId, fileId));
        task.setDeviceId(deviceId);
        task.setChannelId(channelId);
        task.setFileId(fileId);
        task.setFileName(fileName);
        task.setFileSize(fileSize);
        task.setStartTime(startTime);
        task.setEndTime(endTime);
        task.setRangeStart(0);
        task.setRangeEnd(fileSize - 1);
        
        activeTasks.put(task.getTaskId(), task);
        log.info("Download task created: taskId={}, fileName={}, size={}", 
                 task.getTaskId(), fileName, fileSize);
        
        return task;
    }
    
    /**
     * 启动下载任务
     */
    public void startDownload(String taskId, Channel deviceChannel, Channel clientChannel) {
        DownloadTask task = activeTasks.get(taskId);
        if (task == null) {
            log.error("Download task not found: {}", taskId);
            return;
        }
        
        if (task.isDownloading()) {
            log.warn("Download task already in progress: {}", taskId);
            return;
        }
        
        task.setDownloading(true);
        task.setFailed(false);
        task.setCompleted(false);
        task.setTransferStartTime(System.currentTimeMillis());
        
        downloadExecutor.submit(() -> {
            try {
                executeDownload(task, deviceChannel, clientChannel);
            } catch (Exception e) {
                log.error("Download failed: taskId={}", taskId, e);
                task.setFailed(true);
                task.setErrorMessage(e.getMessage());
            } finally {
                task.setDownloading(false);
                if (!task.isFailed() && !task.isCompleted()) {
                    task.setFailed(true);
                    task.setErrorMessage("Download interrupted");
                }
            }
        });
    }
    
    /**
     * 执行下载
     */
    private void executeDownload(DownloadTask task, Channel deviceChannel, Channel clientChannel) {
        log.info("Starting download: taskId={}, range=[{}-{}]", 
                 task.getTaskId(), task.getRangeStart(), task.getRangeEnd());
        
        // TODO: 向设备发送下载请求信令
        // 实际实现需要构造大华私有协议的下载请求包
        
        long totalDownloaded = 0;
        long contentLength = task.getRangeEnd() - task.getRangeStart() + 1;
        
        try {
            // 模拟从设备接收数据并转发给客户端
            // 实际实现需要从设备通道读取二进制流
            
            while (totalDownloaded < contentLength && task.isDownloading()) {
                // 模拟读取数据块（实际应从设备channel读取）
                int chunkSize = Math.min(65536, (int) Math.min(contentLength - totalDownloaded, 65536));
                
                // TODO: 从设备读取实际数据
                // ByteBuf data = readFromDevice(deviceChannel, chunkSize);
                
                // 模拟数据
                byte[] dummyData = new byte[chunkSize];
                
                // 更新进度
                task.updateTransferStats(chunkSize);
                totalDownloaded += chunkSize;
                
                // 发送给客户端
                if (clientChannel != null && clientChannel.isActive()) {
                    ByteBuf buf = clientChannel.alloc().buffer(dummyData.length);
                    buf.writeBytes(dummyData);
                    clientChannel.writeAndFlush(buf);
                }
                
                log.debug("Download progress: {}/{} ({:.2f}%)", 
                         totalDownloaded, contentLength, 
                         (double) totalDownloaded / contentLength * 100);
                
                // 检查是否超时
                if (task.isTimeout()) {
                    throw new RuntimeException("Download timeout");
                }
            }
            
            task.setCompleted(true);
            log.info("Download completed: taskId={}, totalBytes={}, speed={} B/s", 
                     task.getTaskId(), totalDownloaded, task.getTransferSpeed());
            
        } catch (Exception e) {
            log.error("Download error: taskId={}", task.getTaskId(), e);
            throw e;
        }
    }
    
    /**
     * 取消下载任务
     */
    public void cancelDownload(String taskId) {
        DownloadTask task = activeTasks.get(taskId);
        if (task != null) {
            task.setDownloading(false);
            log.info("Download cancelled: taskId={}", taskId);
        }
    }
    
    /**
     * 获取下载任务状态
     */
    public DownloadTask getTaskStatus(String taskId) {
        return activeTasks.get(taskId);
    }
    
    /**
     * 移除已完成的任务
     */
    public void removeTask(String taskId) {
        activeTasks.remove(taskId);
    }
    
    /**
     * 生成任务ID
     */
    private String generateTaskId(String deviceId, String fileId) {
        return deviceId + "_" + fileId + "_" + System.currentTimeMillis();
    }
    
    /**
     * 清理超时任务
     */
    public void cleanupTimeoutTasks() {
        activeTasks.entrySet().removeIf(entry -> {
            DownloadTask task = entry.getValue();
            return task.isTimeout() || (task.isCompleted() && System.currentTimeMillis() - task.getLastTransferTime() > 3600000);
        });
    }
}
