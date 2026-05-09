package com.genersoft.iot.vmp.dahua.bean;

import lombok.Data;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 大华设备会话上下文
 * 存储DH密钥交换后的AES会话密钥、设备信息、通道列表等
 */
@Data
public class DahuaSessionContext {
    
    private String deviceId;          // 设备ID（序列号）
    private String sessionId;         // 会话ID
    private String deviceIp;          // 设备IP
    private int devicePort;           // 设备端口
    private long connectTime;         // 连接时间
    private long lastHeartbeatTime;   // 最后心跳时间
    private boolean authenticated;    // 是否已认证
    private boolean encrypted;        // 是否已启用加密
    
    // DH密钥交换相关
    private byte[] dhPublicKey;       // 本地DH公钥
    private byte[] deviceDhPublicKey; // 设备DH公钥
    private byte[] sharedSecret;      // 共享秘密
    private byte[] aesKey;            // AES会话密钥 (16/24/32字节)
    private byte[] aesIv;             // AES IV (16字节)
    
    // 设备能力
    private int protocolVersion;      // 协议版本 (2.0/4.0/5.0)
    private boolean supportEncryption;// 是否支持加密传输
    private int audioCodecType;       // 音频编码类型 (G.711/G.726/ADPCM等)
    
    // 通道信息
    private ConcurrentHashMap<Integer, DahuaChannel> channels;
    
    // 对讲会话
    private IntercomSession intercomSession;
    
    // 下载任务
    private ConcurrentHashMap<String, DownloadTask> downloadTasks;
    
    public DahuaSessionContext() {
        this.channels = new ConcurrentHashMap<>();
        this.downloadTasks = new ConcurrentHashMap<>();
        this.connectTime = System.currentTimeMillis();
        this.lastHeartbeatTime = System.currentTimeMillis();
    }
    
    /**
     * 更新心跳时间
     */
    public void updateHeartbeat() {
        this.lastHeartbeatTime = System.currentTimeMillis();
    }
    
    /**
     * 检查会话是否超时（5分钟无心跳）
     */
    public boolean isTimeout() {
        return System.currentTimeMillis() - lastHeartbeatTime > 5 * 60 * 1000;
    }
}
