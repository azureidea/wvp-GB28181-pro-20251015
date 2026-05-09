package com.genersoft.iot.vmp.dahua.bean;

import lombok.Data;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 双向对讲会话
 * 管理设备与服务器之间的音频流传输
 */
@Data
public class IntercomSession {
    
    private String sessionId;           // 对讲会话ID
    private String deviceId;            // 设备ID
    private int channelId;              // 通道ID
    private long startTime;             // 开始时间
    private boolean active;             // 是否活跃
    
    // 音频编码信息
    private int deviceAudioCodec;       // 设备音频编码 (G.711/G.726/ADPCM)
    private int serverAudioCodec;       // 服务器输出编码 (OPUS/AAC for WebRTC)
    private int sampleRate;             // 采样率
    private int channels;               // 声道数
    
    // 转码器状态
    private AtomicBoolean transcoding;  // 是否正在转码
    
    // WebSocket会话ID（用于推送音频到前端）
    private String webSocketSessionId;
    
    public IntercomSession() {
        this.startTime = System.currentTimeMillis();
        this.active = true;
        this.transcoding = new AtomicBoolean(false);
        this.sampleRate = 8000;  // 默认8kHz
        this.channels = 1;       // 默认单声道
    }
    
    /**
     * 关闭对讲会话
     */
    public void close() {
        this.active = false;
        this.transcoding.set(false);
    }
}
