# 大华主动注册协议接入模块

## 概述

本模块实现了大华设备主动注册协议，支持通过ZLMediaKit进行流媒体转发。包含以下核心功能：

- **设备注册与认证**：支持设备主动TCP连接注册，用户名密码验证
- **DH密钥交换加密**：基于Diffie-Hellman的密钥协商，AES-256-CBC加密传输
- **实时预览**：自动调用ZLMediaKit拉取RTSP流并转换为HTTP-FLV/HLS
- **云台控制**：支持方向、速度、变倍、聚焦等PTZ操作
- **预置位管理**：设置、调用、删除预置位
- **录像查询与回放**：按时间范围查询录像文件，支持回放控制
- **双向对讲**：G.711/ADPCM转OPUS/AAC，支持WebRTC前端对讲
- **报警上报**：绊线检测、区域入侵、移动侦测、逆行检测、徘徊检测、人员聚集、声音异常、设备异常等
- **录像下载代理**：支持HTTP Range断点续传，流式下载

## 目录结构

```
dahua/
├── bean/                       # 数据模型
│   ├── DahuaSessionContext.java    # 会话上下文（密钥、设备信息等）
│   ├── DahuaChannel.java           # 通道信息
│   ├── IntercomSession.java        # 对讲会话
│   └── DownloadTask.java           # 下载任务
├── codec/                      # 编解码工具（待实现）
├── handler/                    # Netty处理器
│   └── DahuaServerHandler.java     # 主协议处理器
├── service/                    # 业务服务层
│   ├── DahuaService.java           # 服务接口
│   └── DahuaServiceImpl.java       # 服务实现
├── security/                   # 安全模块
│   ├── DhKeyExchangeUtil.java      # DH密钥交换工具
│   └── AesCryptoUtil.java          # AES加解密工具
├── audio/                      # 音频处理
│   ├── FFmpegLib.java              # FFmpeg JNA接口
│   └── FfmpegAudioTranscoder.java  # 音频转码器
├── download/                   # 下载代理
│   └── DahuaDownloadProxy.java     # 录像下载代理
└── config/                     # 配置类
    ├── DahuaServerConfig.java      # Spring配置
    ├── DahuaServerStarter.java     # 服务器启动器
    └── DahuaChannelInitializer.java # Netty通道初始化
```

## 配置说明

在 `application.yml` 中添加以下配置：

```yaml
dahua:
  enabled: true                     # 是否启用大华接入服务
  port: 9080                        # 监听端口
  boss-threads: 1                   # Netty Boss线程数
  worker-threads: 0                 # Worker线程数（0=CPU核心数*2）
  heartbeat-timeout: 60             # 心跳超时时间（秒）
  
  # 加密配置
  encryption:
    enabled: true                   # 是否启用加密传输
    key-length: 32                  # AES密钥长度（16/24/32）
    
  # 对讲配置
  intercom:
    output-codec: opus              # 输出编码（opus/aac）
    sample-rate: 48000              # 采样率
    channels: 2                     # 声道数
    
  # 下载配置
  download:
    max-concurrent: 5               # 最大并发下载数
    chunk-size: 65536               # 下载块大小（字节）
    timeout: 300                    # 下载超时时间（秒）
```

## 快速开始

### 1. 添加依赖

在 `pom.xml` 中添加：

```xml
<!-- BouncyCastle加密库 -->
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk18on</artifactId>
    <version>1.77</version>
</dependency>

<!-- JNA（用于调用FFmpeg） -->
<dependency>
    <groupId>net.java.dev.jna</groupId>
    <artifactId>jna</artifactId>
    <version>5.13.0</version>
</dependency>

<!-- Netty（已存在） -->
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-all</artifactId>
</dependency>
```

### 2. 安装FFmpeg（对讲功能需要）

```bash
# Ubuntu/Debian
sudo apt-get install ffmpeg libavcodec-dev

# CentOS/RHEL
sudo yum install ffmpeg ffmpeg-devel

# Windows
# 下载 https://ffmpeg.org/download.html 并添加到PATH
```

### 3. 配置设备端

在大华设备上配置：
- 服务器地址：运行本服务的服务器IP
- 服务器端口：9080（或配置的端口）
- 设备ID：唯一标识符（如DH000001）
- 用户名/密码：与系统数据库中一致

### 4. 启动服务

```bash
java -jar wvp-pro.jar
```

查看日志确认服务启动：
```
INFO  Starting Dahua server on port 9080
INFO  Dahua server started successfully on port 9080
```

## 协议流程

### 设备注册流程

```
设备                          服务器
 |                              |
 |--- TCP Connect ------------->|
 |                              |
 |--- 注册请求 (设备ID, 用户，密码) ->|
 |                              |
 |<-- 注册响应 (会话ID) ---------|
 |                              |
 | [可选] DH密钥交换：            |
 |--- 设备公钥 ---------------->|
 |<-- 服务器公钥 ---------------|
 | (双方计算共享秘密，派生AES密钥)  |
 |<-- 加密确认 -----------------|
 |                              |
 |=== 加密通道建立完成 ===      |
 |                              |
 |--- 心跳 (定期) ------------->|
 |<-- 心跳确认 -----------------|
```

### 实时预览流程

```
客户端                        服务器                       设备
 |                              |                           |
 |-- 请求预览 (通道ID) --------->|                           |
 |                              |--- 预览请求 ------------->|
 |                              |<-- RTSP地址 --------------|
 |                              |                           |
 |                              | 调用ZLMediaKit API:        |
 |                              | addFFmpegSource(rtspUrl)  |
 |                              |                           |
 |<-- 播放地址 (http-flv/hls) ---|                           |
 |                              |                           |
 |--- 播放 -------------------->|<====== RTSP流 ============|
 |                              |                           |
```

### 双向对讲流程

```
前端WebSocket                 服务器                       设备
 |                              |                           |
 |-- 开始对讲 ----------------->|                           |
 |                              |--- 对讲开始请求 --------->|
 |                              |<-- 对讲确认 --------------|
 |                              |                           |
 |                              |<== G.711音频流 ===========|
 |                              |                           |
 |                              | [G.711 → PCM → OPUS转码]  |
 |<-- OPUS音频帧 ---------------|                           |
 |                              |                           |
 |== 麦克风音频 (OPUS) =========>|                           |
 |                              | [OPUS → PCM → G.711转码]  |
 |                              |--- 对讲音频数据 --------->|
 |                              |                           |
```

## API接口

### 设备管理

```java
// 获取在线设备列表
GET /api/dahua/devices

// 获取设备详情
GET /api/dahua/devices/{deviceId}

// 获取设备通道列表
GET /api/dahua/devices/{deviceId}/channels
```

### 实时预览

```java
// 请求预览
POST /api/dahua/preview
{
  "deviceId": "DH000001",
  "channelId": 1,
  "streamType": 0  // 0-主码流，1-子码流
}

// 返回
{
  "appId": "dahua_DH000001",
  "streamId": "ch1_main",
  "flvUrl": "http://localhost:8083/dahua_DH000001/ch1_main.live.flv",
  "hlsUrl": "http://localhost:8083/dahua_DH000001/ch1_main/hls.m3u8"
}
```

### 云台控制

```java
// PTZ控制
POST /api/dahua/ptz
{
  "deviceId": "DH000001",
  "channelId": 1,
  "command": "up",      // up/down/left/right/zoom+/zoom-/focus+/focus-
  "speed": 50           // 0-100
}

// 预置位操作
POST /api/dahua/preset
{
  "deviceId": "DH000001",
  "channelId": 1,
  "action": "goto",     // goto/set/delete
  "presetId": 1
}
```

### 录像管理

```java
// 查询录像文件
POST /api/dahua/records/query
{
  "deviceId": "DH000001",
  "channelId": 1,
  "startTime": 1699920000000,
  "endTime": 1699923600000
}

// 下载录像文件
GET /api/dahua/download/{taskId}
Header: Range: bytes=0-1048575  // 支持断点续传
```

### 双向对讲

```java
// WebSocket连接
ws://localhost:8083/api/dahua/intercom?deviceId=DH000001&channelId=1

// 发送音频帧（二进制OPUS）
[WebSocket Binary Message]

// 接收音频帧（二进制OPUS）
[WebSocket Binary Message]
```

## 报警类型支持

| 报警类型 | 代码 | 描述 |
|---------|------|------|
| 绊线检测 | 0x01 | 检测穿越警戒线的目标 |
| 区域入侵 | 0x02 | 检测进入警戒区域的目标 |
| 移动侦测 | 0x03 | 检测画面中的移动物体 |
| 逆行检测 | 0x04 | 检测反向行走的人员 |
| 徘徊检测 | 0x05 | 检测在区域内徘徊的人员 |
| 人员聚集 | 0x06 | 检测人员密度超标 |
| 声音异常 | 0x07 | 检测异常声音（尖叫、破碎等） |
| 设备异常 | 0x08 | 设备离线、遮挡、故障等 |

## 注意事项

1. **加密模式**：建议生产环境启用DH密钥交换加密传输
2. **FFmpeg依赖**：对讲功能需要安装FFmpeg并确保JNA能找到动态库
3. **并发限制**：下载任务有最大并发数限制，避免占用过多带宽
4. **心跳保活**：设备需定期发送心跳，否则会被判定为离线
5. **ZLMediaKit配置**：确保ZLMediaKit服务正常运行且API可访问

## 后续开发计划

- [ ] 完善大华私有协议字节级解析（参考官方SDK）
- [ ] 实现完整的CRC16校验
- [ ] 添加设备远程配置功能（编码参数、图像参数、录像计划等）
- [ ] 支持多路视频流并发转发优化
- [ ] 添加报警联动（截图、录像、推送通知）
- [ ] 实现设备固件升级功能
- [ ] 添加性能监控和日志审计

## 参考资料

- 《大华网络摄像机SDK开发文档》
- 《DH-IPC私有协议规范》
- ZLMediaKit: https://github.com/ZLMediaKit/ZLMediaKit
- Netty: https://netty.io/
- BouncyCastle: https://www.bouncycastle.org/
