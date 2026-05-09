package com.genersoft.iot.vmp.dahua.service;

import com.genersoft.iot.vmp.dahua.bean.*;

/**
 * 大华设备服务接口
 * 处理设备注册、信令解析、业务逻辑等
 */
public interface DahuaService {
    
    /**
     * 认证设备凭证
     */
    boolean authenticateDevice(String deviceId, String username, String password);
    
    /**
     * 设备上线回调
     */
    void onDeviceOnline(DahuaSessionContext sessionCtx);
    
    /**
     * 设备下线回调
     */
    void onDeviceOffline(DahuaSessionContext sessionCtx);
    
    /**
     * 处理资源列表请求
     */
    void handleResourceListRequest(DahuaSessionContext sessionCtx, int seq, byte[] payload);
    
    /**
     * 处理实时预览请求
     */
    void handlePreviewRequest(DahuaSessionContext sessionCtx, int seq, byte[] payload);
    
    /**
     * 处理云台控制请求
     */
    void handlePtzControl(DahuaSessionContext sessionCtx, int seq, byte[] payload);
    
    /**
     * 处理预置位控制请求
     */
    void handlePresetControl(DahuaSessionContext sessionCtx, int seq, byte[] payload);
    
    /**
     * 处理录像查询请求
     */
    void handleRecordQuery(DahuaSessionContext sessionCtx, int seq, byte[] payload);
    
    /**
     * 处理录像回放请求
     */
    void handleRecordPlayback(DahuaSessionContext sessionCtx, int seq, byte[] payload);
    
    /**
     * 处理报警上报
     */
    void handleAlarmReport(DahuaSessionContext sessionCtx, int seq, byte[] payload);
    
    /**
     * 处理对讲开始请求
     */
    void handleIntercomStart(DahuaSessionContext sessionCtx, int seq, byte[] payload);
    
    /**
     * 处理对讲音频数据
     */
    void handleIntercomAudio(DahuaSessionContext sessionCtx, int seq, byte[] payload);
    
    /**
     * 处理设备配置请求
     */
    void handleDeviceConfig(DahuaSessionContext sessionCtx, int seq, byte[] payload);
}
