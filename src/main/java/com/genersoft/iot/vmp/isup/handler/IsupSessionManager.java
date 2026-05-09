package com.genersoft.iot.vmp.isup.handler;

import com.genersoft.iot.vmp.isup.bean.IsupDevice;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ISUP设备会话管理器
 * 管理已注册设备的连接状态和信息
 */
@Slf4j
public class IsupSessionManager {
    
    /**
     * 设备ID -> 设备信息映射
     */
    private final Map<String, IsupDevice> deviceMap = new ConcurrentHashMap<>();
    
    /**
     * Channel -> 设备ID映射
     */
    private final Map<Channel, String> channelMap = new ConcurrentHashMap<>();
    
    /**
     * 注册设备
     * @param device 设备信息
     * @param channel 连接通道
     */
    public synchronized void registerDevice(IsupDevice device, Channel channel) {
        String deviceId = device.getDeviceSerial();
        
        // 如果设备已存在，先清理旧连接
        if (deviceMap.containsKey(deviceId)) {
            IsupDevice oldDevice = deviceMap.get(deviceId);
            log.warn("设备重复注册，清理旧连接：{}", deviceId);
            // 注意：不关闭旧通道，由上层决定如何处理
        }
        
        deviceMap.put(deviceId, device);
        channelMap.put(channel, deviceId);
        
        log.info("设备注册成功：{}, 当前在线设备数：{}", deviceId, deviceMap.size());
    }
    
    /**
     * 注销设备
     * @param deviceId 设备ID
     */
    public synchronized void unregisterDevice(String deviceId) {
        IsupDevice device = deviceMap.remove(deviceId);
        if (device != null) {
            device.setOnline(false);
            // 从channelMap中移除（需要遍历查找）
            channelMap.entrySet().removeIf(entry -> entry.getValue().equals(deviceId));
            log.info("设备注销成功：{}, 当前在线设备数：{}", deviceId, deviceMap.size());
        }
    }
    
    /**
     * 获取设备信息
     * @param deviceId 设备ID
     * @return 设备信息，不存在返回null
     */
    public IsupDevice getDevice(String deviceId) {
        return deviceMap.get(deviceId);
    }
    
    /**
     * 根据Channel获取设备
     * @param channel 连接通道
     * @return 设备信息，不存在返回null
     */
    public IsupDevice getDeviceByChannel(Channel channel) {
        String deviceId = channelMap.get(channel);
        if (deviceId != null) {
            return deviceMap.get(deviceId);
        }
        return null;
    }
    
    /**
     * 更新设备心跳时间
     * @param deviceId 设备ID
     */
    public void updateHeartbeat(String deviceId) {
        IsupDevice device = deviceMap.get(deviceId);
        if (device != null) {
            device.setLastHeartbeatTime(System.currentTimeMillis());
        }
    }
    
    /**
     * 获取所有在线设备
     * @return 在线设备列表
     */
    public Map<String, IsupDevice> getAllDevices() {
        return new ConcurrentHashMap<>(deviceMap);
    }
    
    /**
     * 获取在线设备数量
     * @return 在线设备数
     */
    public int getOnlineDeviceCount() {
        return (int) deviceMap.values().stream().filter(IsupDevice::isOnline).count();
    }
    
    /**
     * 清理超时设备（超过指定时间未心跳）
     * @param timeoutMillis 超时时间（毫秒）
     */
    public void cleanupTimeoutDevices(long timeoutMillis) {
        long now = System.currentTimeMillis();
        deviceMap.entrySet().removeIf(entry -> {
            IsupDevice device = entry.getValue();
            if (now - device.getLastHeartbeatTime() > timeoutMillis) {
                log.info("设备超时下线：{}", entry.getKey());
                device.setOnline(false);
                return true;
            }
            return false;
        });
    }
}
