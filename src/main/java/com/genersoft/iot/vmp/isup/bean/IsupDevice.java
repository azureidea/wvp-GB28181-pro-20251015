package com.genersoft.iot.vmp.isup.bean;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 海康ISUP协议设备实体类
 * 对应《海康威视ISUP协议开发文档》中的设备注册信息结构
 */
@Data
@Slf4j
public class IsupDevice {
    
    /**
     * 设备唯一标识（设备序列号）
     */
    private String deviceSerial;
    
    /**
     * 设备验证码（用于鉴权）
     */
    private String deviceCode;
    
    /**
     * 设备IP地址
     */
    private String ipAddress;
    
    /**
     * 设备端口
     */
    private Integer port;
    
    /**
     * 协议版本（0x01: ISUP 2.0, 0x02: ISUP 3.0）
     */
    private byte protocolVersion;
    
    /**
     * 设备类型（1: IPC, 2: NVR, 3: 编码器，4: 平台）
     */
    private byte deviceType;
    
    /**
     * 通道数量
     */
    private int channelCount;
    
    /**
     * 注册时间戳
     */
    private long registerTime;
    
    /**
     * 最后心跳时间
     */
    private long lastHeartbeatTime;
    
    /**
     * 连接状态（true: 在线，false: 离线）
     */
    private boolean online;
    
    /**
     * ZLMediaKit流ID（拉流时使用）
     */
    private String zlmStreamId;
    
    /**
     * ZLMediaKit应用名
     */
    private String zlmAppName;
    
    /**
     * 是否启用加密（ISUP 3.0支持）
     */
    private boolean encryptionEnabled;
    
    /**
     * 加密密钥（可选，用于消息体加密）
     */
    private byte[] encryptionKey;
    
    /**
     * 设备能力集位图
     * bit0: 支持视频预览
     * bit1: 支持云台控制
     * bit2: 支持录像回放
     * bit3: 支持语音对讲
     * bit4: 支持报警输入
     * bit5: 支持报警输出
     */
    private int capabilities;
    
    /**
     * 厂商代码（海康为0x0001）
     */
    private short vendorCode = 0x0001;
    
    /**
     * 设备型号
     */
    private String deviceModel;
    
    /**
     * 固件版本
     */
    private String firmwareVersion;
    
    /**
     * 获取设备能力描述
     */
    public String getCapabilitiesDescription() {
        StringBuilder sb = new StringBuilder();
        if ((capabilities & 0x01) != 0) sb.append("视频预览 ");
        if ((capabilities & 0x02) != 0) sb.append("云台控制 ");
        if ((capabilities & 0x04) != 0) sb.append("录像回放 ");
        if ((capabilities & 0x08) != 0) sb.append("语音对讲 ");
        if ((capabilities & 0x10) != 0) sb.append("报警输入 ");
        if ((capabilities & 0x20) != 0) sb.append("报警输出 ");
        return sb.toString().trim();
    }
}
