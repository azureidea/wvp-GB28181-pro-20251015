package com.genersoft.iot.vmp.dahua.bean;

import lombok.Data;

/**
 * 大华设备通道信息
 */
@Data
public class DahuaChannel {
    
    private int channelId;        // 通道ID
    private String channelName;   // 通道名称
    private boolean online;       // 是否在线
    private int deviceType;       // 设备类型 (1-IPC, 2-DVR, 3-NVR等)
    
    // 视频流信息
    private String mainStreamUrl;   // 主码流地址
    private String subStreamUrl;    // 子码流地址
    private int videoCodec;         // 视频编码 (H.264/H.265)
    private int resolution;         // 分辨率
    private int frameRate;          // 帧率
    private int bitRate;            // 码率
    
    // PTZ能力
    private boolean supportPtz;           // 支持云台控制
    private boolean supportPreset;        // 支持预置位
    private int presetCount;              // 预置位数量
    
    // 报警输入输出
    private int alarmInCount;             // 报警输入数量
    private int alarmOutCount;            // 报警输出数量
    
    public DahuaChannel() {
        this.online = true;
        this.supportPtz = true;
        this.supportPreset = true;
    }
}
