# GB28181-2022 重构说明

## 概述
本次重构主要针对媒体服务和设备管理模块，增加对GB28181-2022标准的支持。

## 主要变更

### 1. 协议版本支持
- 新增 `GBProtocolVersion` 枚举类，支持2011/2016/2022三个版本
- 在 `Device` 实体类中增加协议版本相关字段：
  - `protocolVersion`: 协议版本标识
  - `supportPrecisionPTZ`: 精准PTZ控制支持
  - `supportCruiseTrack`: 巡航轨迹支持
  - `supportReversePlayback`: 录像倒放支持
  - `supportAuxiliaryStream`: 辅码流支持
  - `supportH265`: H.265编码支持
  - `supportAAC`: AAC音频支持
  - `deviceCapabilities`: 设备能力集

### 2. 设备服务接口增强
在 `IDeviceService` 接口中新增以下方法：

#### 协议版本管理
- `getDeviceProtocolVersion(String deviceId)`: 获取设备协议版本
- `updateDeviceProtocolVersion(String deviceId, String protocolVersion)`: 更新设备协议版本

#### GB28181-2022新特性
- `queryDeviceCapabilities()`: 查询设备能力集
- `precisionPTZControl()`: 精准PTZ控制
- `queryCruiseTrack()`: 巡航轨迹查询
- `formatStorage()`: 存储卡格式化
- `deviceUpgrade()`: 设备软件升级
- `configOSD()`: OSD配置

### 3. 媒体服务接口增强
新增 `IMediaService2022` 接口，提供以下功能：

#### 协议版本感知
- `getProtocolVersion()`: 获取当前协议版本
- `isSupportGB2022()`: 检查是否支持2022特性

#### RTP服务器管理
- `openRTPServer()`: 支持辅码流（streamType=2）
- `closeRTPServer()`: 关闭RTP服务器

#### 录制与回放增强
- `startRecord()`: 支持H.265和AAC编码
- `getRecordFiles()`: 支持倒序排列（GB2022特性）
- `playback()`: 支持倒放功能（GB2022特性）

#### 流媒体转发
- `pushStreamToPlatform()`: 支持H.265+AAC转推
- `pullStreamProxy()`: 支持品牌URL拼接

#### 媒体服务器管理
- 节点管理（添加、删除、更新）
- 负载均衡（获取最优服务器）
- 连通性检查
- 会话清理

## GB28181-2022新特性支持

### 1. 精准PTZ控制
- 支持更精确的云台控制指令
- 支持绝对定位
- 支持变倍、聚焦、光圈精细控制

### 2. 巡航轨迹
- 支持巡航轨迹查询
- 支持巡航计划设置
- 支持预置位联动

### 3. 录像倒放
- 支持录像文件倒序播放
- 支持倍速控制（包括负倍速）

### 4. 辅码流支持
- 在主码流、子码流基础上增加辅码流支持
- 支持三码流同时传输

### 5. H.265+AAC编码
- 支持H.265视频编码
- 支持AAC音频编码
- 向下兼容H.264+G.711

### 6. 设备管理增强
- 存储卡格式化
- 设备软件远程升级
- OSD（字符叠加）配置
- 设备能力集查询

## 兼容性说明

### 向后兼容
- 默认使用GB28181-2016协议
- 旧设备无需修改配置即可继续使用
- 新功能仅在设备支持时启用

### 升级建议
1. 更新设备注册流程，自动识别协议版本
2. 在设备信息同步时获取能力集
3. 根据协议版本动态调整功能菜单
4. 媒体服务器需要升级到支持H.265和AAC的版本

## 数据库变更建议

需要在设备表中增加以下字段：
```sql
ALTER TABLE device ADD COLUMN protocol_version VARCHAR(10) DEFAULT '2016';
ALTER TABLE device ADD COLUMN support_precision_ptz BOOLEAN DEFAULT FALSE;
ALTER TABLE device ADD COLUMN support_cruise_track BOOLEAN DEFAULT FALSE;
ALTER TABLE device ADD COLUMN support_reverse_playback BOOLEAN DEFAULT FALSE;
ALTER TABLE device ADD COLUMN support_auxiliary_stream BOOLEAN DEFAULT FALSE;
ALTER TABLE device ADD COLUMN support_h265 BOOLEAN DEFAULT TRUE;
ALTER TABLE device ADD COLUMN support_aac BOOLEAN DEFAULT FALSE;
ALTER TABLE device ADD COLUMN device_capabilities TEXT;
```

## 后续工作

1. 实现 `IDeviceService` 新增接口方法
2. 实现 `IMediaService2022` 接口
3. 更新SIP信令处理逻辑
4. 添加前端界面支持
5. 编写单元测试
6. 更新文档

## 参考资料
- GB/T 28181-2022 公共安全视频监控联网系统信息传输、交换、控制技术要求
- GB/T 28181-2016 公共安全视频监控联网系统信息传输、交换、控制技术要求
