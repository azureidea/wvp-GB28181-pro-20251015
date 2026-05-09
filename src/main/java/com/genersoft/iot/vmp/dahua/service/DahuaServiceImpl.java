package com.genersoft.iot.vmp.dahua.service;

import com.genersoft.iot.vmp.dahua.bean.*;
import com.genersoft.iot.vmp.dahua.audio.FfmpegAudioTranscoder;
import com.genersoft.iot.vmp.dahua.download.DahuaDownloadProxy;
import com.genersoft.iot.vmp.media.zlm.ZlmRestUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 大华设备服务实现
 */
@Slf4j
@Service
public class DahuaServiceImpl implements DahuaService {
    
    // 会话管理
    private final Map<String, DahuaSessionContext> activeSessions = new ConcurrentHashMap<>();
    
    // 对讲会话管理
    private final Map<String, IntercomSession> intercomSessions = new ConcurrentHashMap<>();
    
    // 音频转码器缓存
    private final Map<String, FfmpegAudioTranscoder> transcoders = new ConcurrentHashMap<>();
    
    // 下载代理
    @Autowired(required = false)
    private DahuaDownloadProxy downloadProxy;
    
    // ZLMediaKit工具
    @Autowired(required = false)
    private ZlmRestUtils zlmRestUtils;
    
    // 模拟设备数据库（实际应从数据库查询）
    private final Map<String, DeviceCredential> deviceDatabase = new ConcurrentHashMap<>();
    
    static class DeviceCredential {
        String deviceId;
        String username;
        String password;
        boolean enabled;
        
        DeviceCredential(String deviceId, String username, String password, boolean enabled) {
            this.deviceId = deviceId;
            this.username = username;
            this.password = password;
            this.enabled = enabled;
        }
    }
    
    public DahuaServiceImpl() {
        // 初始化下载代理（最大并发5个下载任务）
        this.downloadProxy = new DahuaDownloadProxy(5);
        
        // 模拟添加一些设备凭证（实际应从数据库加载）
        deviceDatabase.put("DH000001", new DeviceCredential("DH000001", "admin", "admin123", true));
        deviceDatabase.put("DH000002", new DeviceCredential("DH000002", "admin", "admin456", true));
    }
    
    @Override
    public boolean authenticateDevice(String deviceId, String username, String password) {
        DeviceCredential cred = deviceDatabase.get(deviceId);
        if (cred == null || !cred.enabled) {
            return false;
        }
        return cred.username.equals(username) && cred.password.equals(password);
    }
    
    @Override
    public void onDeviceOnline(DahuaSessionContext sessionCtx) {
        activeSessions.put(sessionCtx.getSessionId(), sessionCtx);
        log.info("Device online: deviceId={}, sessionId={}, encrypted={}", 
                 sessionCtx.getDeviceId(), sessionCtx.getSessionId(), sessionCtx.isEncrypted());
        
        // 请求设备资源列表
        // requestResourceList(sessionCtx);
    }
    
    @Override
    public void onDeviceOffline(DahuaSessionContext sessionCtx) {
        activeSessions.remove(sessionCtx.getSessionId());
        
        // 清理对讲会话
        if (sessionCtx.getIntercomSession() != null) {
            String intercomId = sessionCtx.getIntercomSession().getSessionId();
            intercomSessions.remove(intercomId);
            FfmpegAudioTranscoder transcoder = transcoders.remove(intercomId);
            if (transcoder != null) {
                transcoder.stop();
            }
        }
        
        log.info("Device offline: deviceId={}", sessionCtx.getDeviceId());
    }
    
    @Override
    public void handleResourceListRequest(DahuaSessionContext sessionCtx, int seq, byte[] payload) {
        log.debug("Resource list request from device: {}", sessionCtx.getDeviceId());
        
        // 构建资源列表响应
        // 实际实现需要查询设备通道信息并返回
        List<DahuaChannel> channels = getDeviceChannels(sessionCtx.getDeviceId());
        
        // 将通道信息添加到会话上下文
        for (DahuaChannel channel : channels) {
            sessionCtx.getChannels().put(channel.getChannelId(), channel);
        }
        
        // 发送响应（示例）
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(channels.size()); // 通道数量
        
        for (DahuaChannel channel : channels) {
            byte[] channelIdBytes = String.valueOf(channel.getChannelId()).getBytes();
            buffer.putShort((short) channelIdBytes.length);
            buffer.put(channelIdBytes);
            
            byte[] channelNameBytes = channel.getChannelName().getBytes();
            buffer.putShort((short) channelNameBytes.length);
            buffer.put(channelNameBytes);
            
            buffer.put(channel.isOnline() ? (byte) 0x01 : (byte) 0x00);
            // ... 其他字段
        }
        
        // 发送响应包
        // sendResponse(sessionCtx, 0x0201, seq, buffer.array());
        
        log.info("Sent resource list to device: {} channels", channels.size());
    }
    
    @Override
    public void handlePreviewRequest(DahuaSessionContext sessionCtx, int seq, byte[] payload) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            buffer.order(ByteOrder.BIG_ENDIAN);
            
            short channelIdLen = buffer.getShort();
            byte[] channelIdBytes = new byte[channelIdLen];
            buffer.get(channelIdBytes);
            int channelId = Integer.parseInt(new String(channelIdBytes));
            
            short streamType = buffer.getShort(); // 0-主码流，1-子码流
            
            log.info("Preview request: deviceId={}, channelId={}, streamType={}", 
                     sessionCtx.getDeviceId(), channelId, streamType);
            
            // 获取通道信息
            DahuaChannel channel = sessionCtx.getChannels().get(channelId);
            if (channel == null) {
                log.error("Channel not found: {}", channelId);
                return;
            }
            
            // 调用ZLMediaKit创建拉流任务
            if (zlmRestUtils != null) {
                String rtspUrl = streamType == 0 ? channel.getMainStreamUrl() : channel.getSubStreamUrl();
                
                // 生成应用ID和流ID
                String appId = "dahua_" + sessionCtx.getDeviceId();
                String streamId = "ch" + channelId + "_" + (streamType == 0 ? "main" : "sub");
                
                // 调用ZLM API开始拉流
                boolean success = zlmRestUtils.addFFmpegSource(rtspUrl, appId, streamId);
                
                if (success) {
                    log.info("Started pull stream: appId={}, streamId={}", appId, streamId);
                    
                    // 返回流地址给客户端
                    // sendPreviewResponse(sessionCtx, seq, appId, streamId);
                } else {
                    log.error("Failed to start pull stream");
                }
            }
            
        } catch (Exception e) {
            log.error("Error handling preview request", e);
        }
    }
    
    @Override
    public void handlePtzControl(DahuaSessionContext sessionCtx, int seq, byte[] payload) {
        // 解析云台控制命令（方向、速度等）
        // 转发给设备
        log.debug("PTZ control request received");
    }
    
    @Override
    public void handlePresetControl(DahuaSessionContext sessionCtx, int seq, byte[] payload) {
        // 预置位控制（设置、调用、删除）
        log.debug("Preset control request received");
    }
    
    @Override
    public void handleRecordQuery(DahuaSessionContext sessionCtx, int seq, byte[] payload) {
        // 录像文件查询
        log.debug("Record query request received");
    }
    
    @Override
    public void handleRecordPlayback(DahuaSessionContext sessionCtx, int seq, byte[] payload) {
        // 录像回放控制
        log.debug("Record playback request received");
    }
    
    @Override
    public void handleAlarmReport(DahuaSessionContext sessionCtx, int seq, byte[] payload) {
        // 解析报警类型和内容
        // 支持：绊线检测、区域入侵、移动侦测、逆行检测、徘徊检测、人员聚集、声音异常、设备异常等
        log.debug("Alarm report received");
        
        // TODO: 解析报警数据并发布到消息队列
    }
    
    @Override
    public void handleIntercomStart(DahuaSessionContext sessionCtx, int seq, byte[] payload) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            buffer.order(ByteOrder.BIG_ENDIAN);
            
            short channelIdLen = buffer.getShort();
            byte[] channelIdBytes = new byte[channelIdLen];
            buffer.get(channelIdBytes);
            int channelId = Integer.parseInt(new String(channelIdBytes));
            
            log.info("Intercom start request: deviceId={}, channelId={}", 
                     sessionCtx.getDeviceId(), channelId);
            
            // 创建对讲会话
            IntercomSession intercomSession = new IntercomSession();
            intercomSession.setSessionId(generateIntercomSessionId());
            intercomSession.setDeviceId(sessionCtx.getDeviceId());
            intercomSession.setChannelId(channelId);
            intercomSession.setDeviceAudioCodec(sessionCtx.getAudioCodecType());
            intercomSession.setServerAudioCodec(86018); // OPUS
            
            sessionCtx.setIntercomSession(intercomSession);
            intercomSessions.put(intercomSession.getSessionId(), intercomSession);
            
            // 创建音频转码器
            FfmpegAudioTranscoder transcoder = new FfmpegAudioTranscoder(
                intercomSession,
                sessionCtx.getAudioCodecType(),
                86018, // OPUS
                (audioData, timestamp) -> {
                    // 推送音频到WebSocket前端
                    pushAudioToWebSocket(intercomSession, audioData, timestamp);
                }
            );
            
            transcoders.put(intercomSession.getSessionId(), transcoder);
            transcoder.start();
            
            // 发送对讲开始确认
            // sendIntercomStartResponse(sessionCtx, seq, intercomSession.getSessionId());
            
            log.info("Intercom session started: {}", intercomSession.getSessionId());
            
        } catch (Exception e) {
            log.error("Error starting intercom", e);
        }
    }
    
    @Override
    public void handleIntercomAudio(DahuaSessionContext sessionCtx, int seq, byte[] payload) {
        // 接收设备音频数据并提交给转码器
        IntercomSession intercomSession = sessionCtx.getIntercomSession();
        if (intercomSession != null) {
            FfmpegAudioTranscoder transcoder = transcoders.get(intercomSession.getSessionId());
            if (transcoder != null) {
                transcoder.submitAudioData(payload);
            }
        }
    }
    
    @Override
    public void handleDeviceConfig(DahuaSessionContext sessionCtx, int seq, byte[] payload) {
        // 设备配置（名称、循环录像、编码参数、图像参数等）
        log.debug("Device config request received");
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 获取设备通道列表（模拟）
     */
    private List<DahuaChannel> getDeviceChannels(String deviceId) {
        List<DahuaChannel> channels = new ArrayList<>();
        
        // 模拟返回4个通道
        for (int i = 1; i <= 4; i++) {
            DahuaChannel channel = new DahuaChannel();
            channel.setChannelId(i);
            channel.setChannelName("Channel " + i);
            channel.setOnline(true);
            channel.setDeviceType(1); // IPC
            
            // 模拟RTSP地址
            channel.setMainStreamUrl("rtsp://192.168.1." + i + ":554/cam/realmonitor?channel=" + i + "&subtype=0");
            channel.setSubStreamUrl("rtsp://192.168.1." + i + ":554/cam/realmonitor?channel=" + i + "&subtype=1");
            
            channels.add(channel);
        }
        
        return channels;
    }
    
    /**
     * 生成对讲会话ID
     */
    private String generateIntercomSessionId() {
        return "intercom_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
    }
    
    /**
     * 推送音频到WebSocket前端
     */
    private void pushAudioToWebSocket(IntercomSession session, byte[] audioData, long timestamp) {
        // TODO: 通过WebSocket推送音频数据到前端
        // 实际实现需要集成WebSocket会话管理
        log.debug("Pushing audio to WebSocket: sessionId={}, size={}, timestamp={}", 
                 session.getSessionId(), audioData.length, timestamp);
    }
}
