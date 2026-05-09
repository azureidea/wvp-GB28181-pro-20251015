package com.genersoft.iot.vmp.media.service;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.protocol.GBProtocolVersion;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;

/**
 * 媒体服务接口 - 支持GB28181-2022
 */
public interface IMediaService {

    /**
     * 获取协议版本
     * @return 协议版本
     */
    GBProtocolVersion getProtocolVersion();

    /**
     * 检查是否支持GB28181-2022特性
     * @return true-支持，false-不支持
     */
    boolean isSupportGB2022();

    /**
     * 开启RTP服务器
     * @param device 设备信息
     * @param channelId 通道ID
     * @param streamType 流类型（0-主码流，1-子码流，2-辅码流）
     * @param useTcp 是否使用TCP
     * @return RTP服务器信息
     */
    SSRCInfo openRTPServer(Device device, String channelId, int streamType, boolean useTcp);

    /**
     * 关闭RTP服务器
     * @param device 设备信息
     * @param channelId 通道ID
     * @param streamType 流类型
     */
    void closeRTPServer(Device device, String channelId, int streamType);

    /**
     * 开始录制（支持H.265和AAC）
     * @param mediaServer 媒体服务器
     * @param app 应用名
     * @param streamId 流ID
     * @param protocolVersion 协议版本
     * @return 录制结果
     */
    boolean startRecord(MediaServer mediaServer, String app, String streamId, GBProtocolVersion protocolVersion);

    /**
     * 停止录制
     * @param mediaServer 媒体服务器
     * @param app 应用名
     * @param streamId 流ID
     * @return 录制结果
     */
    boolean stopRecord(MediaServer mediaServer, String app, String streamId);

    /**
     * 获取录制文件列表（支持倒序排列-GB2022特性）
     * @param mediaServer 媒体服务器
     * @param app 应用名
     * @param streamId 流ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param reverseOrder 是否倒序（GB2022特性）
     * @return 录制文件列表
     */
    Object getRecordFiles(MediaServer mediaServer, String app, String streamId, 
                         long startTime, long endTime, boolean reverseOrder);

    /**
     * 录像回放（支持倒放-GB2022特性）
     * @param mediaServer 媒体服务器
     * @param app 应用名
     * @param streamId 流ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param reversePlayback 是否倒放（GB2022特性）
     * @param speed 播放速度
     * @return 回放结果
     */
    Object playback(MediaServer mediaServer, String app, String streamId,
                   long startTime, long endTime, boolean reversePlayback, float speed);

    /**
     * 转推流到国标平台（支持H.265+AAC）
     * @param mediaServer 媒体服务器
     * @param app 应用名
     * @param streamId 流ID
     * @param targetUrl 目标地址
     * @param protocolVersion 协议版本
     * @return 转推结果
     */
    boolean pushStreamToPlatform(MediaServer mediaServer, String app, String streamId, 
                                String targetUrl, GBProtocolVersion protocolVersion);

    /**
     * 拉流代理（支持品牌URL拼接）
     * @param mediaServer 媒体服务器
     * @param url 拉流地址
     * @param app 应用名
     * @param streamId 流ID
     * @param brand 设备品牌
     * @return 拉流结果
     */
    Object pullStreamProxy(MediaServer mediaServer, String url, String app, String streamId, String brand);

    /**
     * 获取媒体服务器状态
     * @param mediaServerId 媒体服务器ID
     * @return 媒体服务器状态
     */
    Object getMediaServerStatus(String mediaServerId);

    /**
     * 添加媒体服务器节点
     * @param mediaServer 媒体服务器信息
     * @return 添加结果
     */
    boolean addMediaServerNode(MediaServer mediaServer);

    /**
     * 删除媒体服务器节点
     * @param mediaServerId 媒体服务器ID
     * @return 删除结果
     */
    boolean removeMediaServerNode(String mediaServerId);

    /**
     * 更新媒体服务器配置
     * @param mediaServer 媒体服务器信息
     * @return 更新结果
     */
    boolean updateMediaServerConfig(MediaServer mediaServer);

    /**
     * 检查媒体服务器连通性
     * @param mediaServerId 媒体服务器ID
     * @return true-连通，false-不连通
     */
    boolean checkMediaServerConnectivity(String mediaServerId);

    /**
     * 获取最优媒体服务器（负载均衡）
     * @return 最优媒体服务器
     */
    MediaServer getOptimalMediaServer();

    /**
     * 清理过期会话
     */
    void cleanupExpiredSessions();

    /**
     * 获取SSRC信息
     * @param deviceId 设备ID
     * @param channelId 通道ID
     * @param streamType 流类型
     * @return SSRC信息
     */
    SSRCInfo getSSRCInfo(String deviceId, String channelId, int streamType);

    /**
     * 释放SSRC
     * @param deviceId 设备ID
     * @param channelId 通道ID
     * @param streamType 流类型
     */
    void releaseSSRC(String deviceId, String channelId, int streamType);
}
