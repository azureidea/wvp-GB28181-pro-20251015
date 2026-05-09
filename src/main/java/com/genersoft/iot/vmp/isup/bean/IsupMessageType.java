package com.genersoft.iot.vmp.isup.bean;

/**
 * ISUP协议消息类型定义
 * 对应《海康威视ISUP协议开发文档》第4.3节 消息类型
 */
public class IsupMessageType {
    
    /**
     * 设备注册请求（Device Register Request）
     * 方向：设备 -> 平台
     */
    public static final short DEVICE_REGISTER_REQ = 0x0001;
    
    /**
     * 设备注册响应（Device Register Response）
     * 方向：平台 -> 设备
     */
    public static final short DEVICE_REGISTER_RESP = 0x0002;
    
    /**
     * 设备注销请求（Device Unregister Request）
     * 方向：设备 -> 平台
     */
    public static final short DEVICE_UNREGISTER_REQ = 0x0003;
    
    /**
     * 设备注销响应（Device Unregister Response）
     * 方向：平台 -> 设备
     */
    public static final short DEVICE_UNREGISTER_RESP = 0x0004;
    
    /**
     * 心跳请求（Heartbeat Request）
     * 方向：设备 -> 平台
     */
    public static final short HEARTBEAT_REQ = 0x0010;
    
    /**
     * 心跳响应（Heartbeat Response）
     * 方向：平台 -> 设备
     */
    public static final short HEARTBEAT_RESP = 0x0011;
    
    /**
     * 实时视频预览请求（Live Video Preview Request）
     * 方向：平台 -> 设备
     */
    public static final short LIVE_PREVIEW_REQ = 0x0100;
    
    /**
     * 实时视频预览响应（Live Video Preview Response）
     * 方向：设备 -> 平台
     */
    public static final short LIVE_PREVIEW_RESP = 0x0101;
    
    /**
     * 停止预览请求（Stop Preview Request）
     * 方向：平台 -> 设备
     */
    public static final short STOP_PREVIEW_REQ = 0x0102;
    
    /**
     * 停止预览响应（Stop Preview Response）
     * 方向：设备 -> 平台
     */
    public static final short STOP_PREVIEW_RESP = 0x0103;
    
    /**
     * 云台控制请求（PTZ Control Request）
     * 方向：平台 -> 设备
     */
    public static final short PTZ_CONTROL_REQ = 0x0200;
    
    /**
     * 云台控制响应（PTZ Control Response）
     * 方向：设备 -> 平台
     */
    public static final short PTZ_CONTROL_RESP = 0x0201;
    
    /**
     * 录像回放请求（Playback Request）
     * 方向：平台 -> 设备
     */
    public static final short PLAYBACK_REQ = 0x0300;
    
    /**
     * 录像回放响应（Playback Response）
     * 方向：设备 -> 平台
     */
    public static final short PLAYBACK_RESP = 0x0301;
    
    /**
     * 语音对讲请求（Voice Talk Request）
     * 方向：平台 -> 设备
     */
    public static final short VOICE_TALK_REQ = 0x0400;
    
    /**
     * 语音对讲响应（Voice Talk Response）
     * 方向：设备 -> 平台
     */
    public static final short VOICE_TALK_RESP = 0x0401;
    
    /**
     * 报警输入通知（Alarm Input Notification）
     * 方向：设备 -> 平台
     */
    public static final short ALARM_INPUT_NOTIFY = 0x0500;
    
    /**
     * 报警输入确认（Alarm Input Acknowledge）
     * 方向：平台 -> 设备
     */
    public static final short ALARM_INPUT_ACK = 0x0501;
    
    /**
     * 报警输出控制请求（Alarm Output Control Request）
     * 方向：平台 -> 设备
     */
    public static final short ALARM_OUTPUT_REQ = 0x0510;
    
    /**
     * 报警输出控制响应（Alarm Output Control Response）
     * 方向：设备 -> 平台
     */
    public static final short ALARM_OUTPUT_RESP = 0x0511;
    
    /**
     * 获取设备信息请求（Get Device Info Request）
     * 方向：平台 -> 设备
     */
    public static final short GET_DEVICE_INFO_REQ = 0x0600;
    
    /**
     * 获取设备信息响应（Get Device Info Response）
     * 方向：设备 -> 平台
     */
    public static final short GET_DEVICE_INFO_RESP = 0x0601;
    
    /**
     * 获取通道列表请求（Get Channel List Request）
     * 方向：平台 -> 设备
     */
    public static final short GET_CHANNEL_LIST_REQ = 0x0610;
    
    /**
     * 获取通道列表响应（Get Channel List Response）
     * 方向：设备 -> 平台
     */
    public static final short GET_CHANNEL_LIST_RESP = 0x0611;
    
    /**
     * 流转发通知（Stream Forwarding Notification）
     * 方向：设备 -> 平台
     */
    public static final short STREAM_FORWARD_NOTIFY = 0x0700;
    
    /**
     * 获取消息类型描述
     * @param messageType 消息类型代码
     * @return 描述字符串
     */
    public static String getMessageTypeDescription(short messageType) {
        switch (messageType) {
            case DEVICE_REGISTER_REQ: return "设备注册请求";
            case DEVICE_REGISTER_RESP: return "设备注册响应";
            case DEVICE_UNREGISTER_REQ: return "设备注销请求";
            case DEVICE_UNREGISTER_RESP: return "设备注销响应";
            case HEARTBEAT_REQ: return "心跳请求";
            case HEARTBEAT_RESP: return "心跳响应";
            case LIVE_PREVIEW_REQ: return "实时视频预览请求";
            case LIVE_PREVIEW_RESP: return "实时视频预览响应";
            case STOP_PREVIEW_REQ: return "停止预览请求";
            case STOP_PREVIEW_RESP: return "停止预览响应";
            case PTZ_CONTROL_REQ: return "云台控制请求";
            case PTZ_CONTROL_RESP: return "云台控制响应";
            case PLAYBACK_REQ: return "录像回放请求";
            case PLAYBACK_RESP: return "录像回放响应";
            case VOICE_TALK_REQ: return "语音对讲请求";
            case VOICE_TALK_RESP: return "语音对讲响应";
            case ALARM_INPUT_NOTIFY: return "报警输入通知";
            case ALARM_INPUT_ACK: return "报警输入确认";
            case ALARM_OUTPUT_REQ: return "报警输出控制请求";
            case ALARM_OUTPUT_RESP: return "报警输出控制响应";
            case GET_DEVICE_INFO_REQ: return "获取设备信息请求";
            case GET_DEVICE_INFO_RESP: return "获取设备信息响应";
            case GET_CHANNEL_LIST_REQ: return "获取通道列表请求";
            case GET_CHANNEL_LIST_RESP: return "获取通道列表响应";
            case STREAM_FORWARD_NOTIFY: return "流转发通知";
            default: return String.format("未知消息类型 (0x%04X)", messageType);
        }
    }
}
