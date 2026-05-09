package com.genersoft.iot.vmp.isup.handler;

import com.genersoft.iot.vmp.isup.bean.*;
import com.genersoft.iot.vmp.isup.codec.IsupCrcUtil;
import com.genersoft.iot.vmp.isup.codec.IsupEncryptionUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * 海康ISUP协议Netty处理器
 * 实现《海康威视ISUP协议开发文档》定义的完整协议解析逻辑
 * 
 * 协议特点：
 * - 字节序：大端模式（Big-Endian）
 * - 消息头固定32字节
 * - CRC16校验（MODBUS多项式）
 * - 可选AES加密（ISUP 3.0）
 */
@Slf4j
public class IsupServerHandler extends ChannelInboundHandlerAdapter {
    
    /**
     * 起始标识（2字节）：0x687A
     */
    private static final short START_FLAG = 0x687A;
    
    /**
     * 最小消息长度（消息头32字节 + 最少消息体）
     */
    private static final int MIN_MESSAGE_LENGTH = 32;
    
    /**
     * 最大消息长度限制（防止恶意攻击）
     */
    private static final int MAX_MESSAGE_LENGTH = 1024 * 1024; // 1MB
    
    /**
     * 设备会话管理器（实际项目中应注入Spring Bean）
     */
    private IsupSessionManager sessionManager;
    
    public IsupServerHandler() {
        this.sessionManager = new IsupSessionManager();
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof ByteBuf)) {
            super.channelRead(ctx, msg);
            return;
        }
        
        ByteBuf buffer = (ByteBuf) msg;
        
        try {
            // 确保使用大端模式读取
            buffer.order(ByteOrder.BIG_ENDIAN);
            
            // 检查是否有足够的数据读取消息头
            while (buffer.readableBytes() >= IsupMessageHeader.HEADER_LENGTH) {
                // 标记当前读取位置
                buffer.markReaderIndex();
                
                // 读取起始标识
                short startFlag = buffer.readShort();
                
                // 验证起始标识
                if (startFlag != START_FLAG) {
                    log.warn("无效的起始标识：0x{:04X}，期望：0x{:04X}，丢弃1字节重新同步", 
                             startFlag, START_FLAG);
                    buffer.resetReaderIndex();
                    buffer.skipBytes(1); // 跳过1字节，尝试重新同步
                    continue;
                }
                
                // 回退2字节，准备完整解析
                buffer.resetReaderIndex();
                
                // 解析消息头
                byte[] headerBytes = new byte[IsupMessageHeader.HEADER_LENGTH];
                buffer.readBytes(headerBytes);
                
                IsupMessageHeader header = IsupMessageHeader.fromBytes(headerBytes, 0);
                
                // 验证消息长度
                if (header.getMessageLength() < MIN_MESSAGE_LENGTH || 
                    header.getMessageLength() > MAX_MESSAGE_LENGTH) {
                    log.error("无效的消息长度：{}，关闭连接", header.getMessageLength());
                    ctx.close();
                    return;
                }
                
                // 计算需要读取的消息体长度（总长度 - 消息头长度）
                int bodyLength = header.getMessageLength() - IsupMessageHeader.HEADER_LENGTH;
                
                // 检查是否有足够的数据读取消息体
                if (buffer.readableBytes() < bodyLength) {
                    // 数据不完整，回退并等待更多数据
                    buffer.resetReaderIndex();
                    break;
                }
                
                // 读取消息体
                byte[] bodyBytes = new byte[bodyLength];
                buffer.readBytes(bodyBytes);
                
                // 读取CRC校验码（2字节）
                short receivedCrc = buffer.readShort();
                
                // 验证CRC校验码
                byte[] crcData = new byte[headerBytes.length + bodyBytes.length];
                System.arraycopy(headerBytes, 0, crcData, 0, headerBytes.length);
                System.arraycopy(bodyBytes, 0, crcData, headerBytes.length, bodyBytes.length);
                
                int calculatedCrc = IsupCrcUtil.calculateCrc16(crcData);
                
                if (receivedCrc != calculatedCrc) {
                    log.error("CRC校验失败：接收=0x{:04X}, 计算=0x{:04X}，丢弃消息", 
                             receivedCrc, calculatedCrc);
                    continue;
                }
                
                // 处理加密（如果需要解密）
                byte[] decryptedBody = bodyBytes;
                if (header.getEncryptionFlag() == 0x01) {
                    log.debug("消息已加密，尝试解密...");
                    // 从会话中获取密钥（实际项目应从设备信息中获取）
                    IsupDevice device = sessionManager.getDevice(header.getSourceDeviceId());
                    if (device != null && device.getEncryptionKey() != null) {
                        decryptedBody = IsupEncryptionUtil.decrypt(
                            bodyBytes, 
                            device.getEncryptionKey(), 
                            header.getEncryptionFlag()
                        );
                    } else {
                        log.warn("设备未配置加密密钥，使用默认密钥解密");
                        decryptedBody = IsupEncryptionUtil.decrypt(
                            bodyBytes, 
                            IsupEncryptionUtil.generateDefaultKey(), 
                            header.getEncryptionFlag()
                        );
                    }
                }
                
                // 根据消息类型分发处理
                handleMessage(ctx, header, decryptedBody);
            }
        } catch (Exception e) {
            log.error("处理ISUP消息时发生异常", e);
            // 发生异常时关闭连接
            ctx.close();
        } finally {
            // 释放缓冲区
            buffer.release();
        }
    }
    
    /**
     * 根据消息类型分发处理
     */
    private void handleMessage(ChannelHandlerContext ctx, IsupMessageHeader header, byte[] body) {
        short messageType = header.getMessageType();
        
        log.debug("收到ISUP消息：类型={}, 序列号={}, 源={}, 目标={}", 
                 IsupMessageType.getMessageTypeDescription(messageType),
                 header.getSequenceNumber(),
                 header.getSourceDeviceId(),
                 header.getTargetDeviceId());
        
        switch (messageType) {
            case IsupMessageType.DEVICE_REGISTER_REQ:
                handleDeviceRegister(ctx, header, body);
                break;
                
            case IsupMessageType.HEARTBEAT_REQ:
                handleHeartbeat(ctx, header, body);
                break;
                
            case IsupMessageType.DEVICE_UNREGISTER_REQ:
                handleDeviceUnregister(ctx, header, body);
                break;
                
            case IsupMessageType.ALARM_INPUT_NOTIFY:
                handleAlarmInput(ctx, header, body);
                break;
                
            case IsupMessageType.STREAM_FORWARD_NOTIFY:
                handleStreamForward(ctx, header, body);
                break;
                
            default:
                log.warn("未处理的ISUP消息类型：0x{:04X}", messageType);
                sendErrorResponse(ctx, header, (short) 0x0100, "Unsupported message type");
        }
    }
    
    /**
     * 处理设备注册请求
     * 对应《海康威视ISUP协议开发文档》第5.1节 设备注册
     */
    private void handleDeviceRegister(ChannelHandlerContext ctx, IsupMessageHeader header, byte[] body) {
        log.info("处理设备注册请求：{}", header.getSourceDeviceId());
        
        try {
            // 解析注册请求体
            // 注册请求体结构（参考ISUP协议文档）：
            // - 设备序列号（48字节，ASCII）
            // - 设备验证码（48字节，ASCII）
            // - 设备类型（1字节）
            // - 通道数量（2字节）
            // - 协议版本（1字节）
            // - 保留位（若干）
            
            if (body.length < 100) {
                log.error("注册请求体长度不足：{}", body.length);
                sendErrorResponse(ctx, header, (short) 0x0200, "Invalid register body length");
                return;
            }
            
            // 提取设备信息（大端模式）
            int offset = 0;
            
            // 设备序列号（48字节）
            byte[] serialBytes = Arrays.copyOfRange(body, offset, Math.min(offset + 48, body.length));
            String deviceSerial = new String(serialBytes).replaceAll("\\x00+$", "").trim();
            offset += 48;
            
            // 设备验证码（48字节）
            byte[] codeBytes = Arrays.copyOfRange(body, offset, Math.min(offset + 48, body.length));
            String deviceCode = new String(codeBytes).replaceAll("\\x00+$", "").trim();
            offset += 48;
            
            // 设备类型（1字节）
            byte deviceType = body[offset++];
            
            // 通道数量（2字节，大端）
            int channelCount = ((body[offset++] & 0xFF) << 8) | (body[offset++] & 0xFF);
            
            // 协议版本（1字节）
            byte protocolVersion = body[offset++];
            
            // 创建设备对象
            IsupDevice device = new IsupDevice();
            device.setDeviceSerial(deviceSerial);
            device.setDeviceCode(deviceCode);
            device.setDeviceType(deviceType);
            device.setChannelCount(channelCount);
            device.setProtocolVersion(protocolVersion);
            device.setIpAddress(ctx.channel().remoteAddress().toString().substring(1).split(":")[0]);
            device.setRegisterTime(System.currentTimeMillis());
            device.setLastHeartbeatTime(System.currentTimeMillis());
            device.setOnline(true);
            
            // 提取设备能力集（如果存在）
            if (body.length > offset + 4) {
                device.setCapabilities(
                    ((body[offset++] & 0xFF) << 24) |
                    ((body[offset++] & 0xFF) << 16) |
                    ((body[offset++] & 0xFF) << 8) |
                    (body[offset++] & 0xFF)
                );
            }
            
            // 保存设备会话
            sessionManager.registerDevice(device, ctx.channel());
            
            log.info("设备注册成功：{}, 类型={}, 通道数={}, 能力={}", 
                     deviceSerial, deviceType, channelCount, device.getCapabilitiesDescription());
            
            // 发送注册响应
            sendDeviceRegisterResponse(ctx, header, device);
            
        } catch (Exception e) {
            log.error("处理设备注册失败", e);
            sendErrorResponse(ctx, header, (short) 0x0201, "Register processing error: " + e.getMessage());
        }
    }
    
    /**
     * 发送设备注册响应
     */
    private void sendDeviceRegisterResponse(ChannelHandlerContext ctx, IsupMessageHeader requestHeader, IsupDevice device) {
        // 构建响应消息头
        IsupMessageHeader responseHeader = new IsupMessageHeader();
        responseHeader.setMessageType(IsupMessageType.DEVICE_REGISTER_RESP);
        responseHeader.setSequenceNumber(requestHeader.getSequenceNumber());
        responseHeader.setSourceDeviceId(requestHeader.getTargetDeviceId()); // 平台ID
        responseHeader.setTargetDeviceId(requestHeader.getSourceDeviceId()); // 设备ID
        responseHeader.setProtocolVersion(device.getProtocolVersion());
        responseHeader.setEncryptionFlag((byte) 0x00); // 暂时不加密
        
        // 构建响应体
        // 响应体结构：
        // - 结果码（2字节）：0x0000-成功
        // - 平台ID（48字节）
        // - 保留位
        
        byte[] responseBody = new byte[52];
        int offset = 0;
        
        // 结果码（成功）
        responseBody[offset++] = 0x00;
        responseBody[offset++] = 0x00;
        
        // 平台ID（48字节，用0填充）
        byte[] platformId = "VMP_PLATFORM".getBytes();
        for (int i = 0; i < 48; i++) {
            responseBody[offset++] = i < platformId.length ? platformId[i] : 0x00;
        }
        
        // 发送响应
        sendMessage(ctx, responseHeader, responseBody);
    }
    
    /**
     * 处理心跳请求
     */
    private void handleHeartbeat(ChannelHandlerContext ctx, IsupMessageHeader header, byte[] body) {
        String deviceId = header.getSourceDeviceId();
        log.debug("收到心跳：{}", deviceId);
        
        // 更新最后心跳时间
        IsupDevice device = sessionManager.getDevice(deviceId);
        if (device != null) {
            device.setLastHeartbeatTime(System.currentTimeMillis());
        } else {
            log.warn("收到未知设备的心跳：{}", deviceId);
        }
        
        // 发送心跳响应
        IsupMessageHeader responseHeader = new IsupMessageHeader();
        responseHeader.setMessageType(IsupMessageType.HEARTBEAT_RESP);
        responseHeader.setSequenceNumber(header.getSequenceNumber());
        responseHeader.setSourceDeviceId(header.getTargetDeviceId());
        responseHeader.setTargetDeviceId(header.getSourceDeviceId());
        responseHeader.setProtocolVersion(header.getProtocolVersion());
        responseHeader.setEncryptionFlag((byte) 0x00);
        
        // 心跳响应体为空
        sendMessage(ctx, responseHeader, new byte[0]);
    }
    
    /**
     * 处理设备注销请求
     */
    private void handleDeviceUnregister(ChannelHandlerContext ctx, IsupMessageHeader header, byte[] body) {
        String deviceId = header.getSourceDeviceId();
        log.info("处理设备注销：{}", deviceId);
        
        // 移除设备会话
        sessionManager.unregisterDevice(deviceId);
        
        // 发送注销响应
        IsupMessageHeader responseHeader = new IsupMessageHeader();
        responseHeader.setMessageType(IsupMessageType.DEVICE_UNREGISTER_RESP);
        responseHeader.setSequenceNumber(header.getSequenceNumber());
        responseHeader.setSourceDeviceId(header.getTargetDeviceId());
        responseHeader.setTargetDeviceId(header.getSourceDeviceId());
        responseHeader.setProtocolVersion(header.getProtocolVersion());
        responseHeader.setEncryptionFlag((byte) 0x00);
        
        byte[] responseBody = new byte[2];
        responseBody[0] = 0x00; // 结果码：成功
        responseBody[1] = 0x00;
        
        sendMessage(ctx, responseHeader, responseBody);
    }
    
    /**
     * 处理报警输入通知
     */
    private void handleAlarmInput(ChannelHandlerContext ctx, IsupMessageHeader header, byte[] body) {
        String deviceId = header.getSourceDeviceId();
        log.info("收到报警输入：{}", deviceId);
        
        // TODO: 解析报警信息并触发业务逻辑
        // 报警体结构：
        // - 报警类型（2字节）
        // - 报警通道号（2字节）
        // - 报警时间（8字节）
        // - 报警状态（1字节）
        
        // 发送确认响应
        IsupMessageHeader responseHeader = new IsupMessageHeader();
        responseHeader.setMessageType(IsupMessageType.ALARM_INPUT_ACK);
        responseHeader.setSequenceNumber(header.getSequenceNumber());
        responseHeader.setSourceDeviceId(header.getTargetDeviceId());
        responseHeader.setTargetDeviceId(header.getSourceDeviceId());
        
        byte[] responseBody = new byte[2];
        responseBody[0] = 0x00;
        responseBody[1] = 0x00;
        
        sendMessage(ctx, responseHeader, responseBody);
    }
    
    /**
     * 处理流转发通知
     */
    private void handleStreamForward(ChannelHandlerContext ctx, IsupMessageHeader header, byte[] body) {
        String deviceId = header.getSourceDeviceId();
        log.info("收到流转发通知：{}", deviceId);
        
        // TODO: 解析流信息并启动ZLMediaKit拉流
        // 流转发体结构：
        // - 通道号（2字节）
        // - 流类型（1字节）
        // - 传输协议（1字节）
        // - IP地址（16字节）
        // - 端口（2字节）
        // - 流ID（64字节）
        
        // 这里应该调用ZLMediaKit API创建拉流任务
        // zlmService.addStreamProxy(...);
        
        // 发送确认响应
        IsupMessageHeader responseHeader = new IsupMessageHeader();
        responseHeader.setMessageType(IsupMessageType.HEARTBEAT_RESP); // 临时使用心跳响应
        responseHeader.setSequenceNumber(header.getSequenceNumber());
        responseHeader.setSourceDeviceId(header.getTargetDeviceId());
        responseHeader.setTargetDeviceId(header.getSourceDeviceId());
        
        byte[] responseBody = new byte[2];
        responseBody[0] = 0x00;
        responseBody[1] = 0x00;
        
        sendMessage(ctx, responseHeader, responseBody);
    }
    
    /**
     * 发送错误响应
     */
    private void sendErrorResponse(ChannelHandlerContext ctx, IsupMessageHeader requestHeader, 
                                   short errorCode, String errorMessage) {
        log.warn("发送错误响应：code={}, msg={}", errorCode, errorMessage);
        
        IsupMessageHeader responseHeader = new IsupMessageHeader();
        responseHeader.setMessageType((short) (requestHeader.getMessageType() + 1)); // 响应类型=请求类型+1
        responseHeader.setSequenceNumber(requestHeader.getSequenceNumber());
        responseHeader.setSourceDeviceId(requestHeader.getTargetDeviceId());
        responseHeader.setTargetDeviceId(requestHeader.getSourceDeviceId());
        responseHeader.setProtocolVersion(requestHeader.getProtocolVersion());
        responseHeader.setEncryptionFlag((byte) 0x00);
        
        // 错误响应体：错误码（2字节）+ 错误信息
        byte[] errorBody = errorMessage.getBytes();
        byte[] responseBody = new byte[2 + errorBody.length];
        responseBody[0] = (byte) ((errorCode >> 8) & 0xFF);
        responseBody[1] = (byte) (errorCode & 0xFF);
        System.arraycopy(errorBody, 0, responseBody, 2, errorBody.length);
        
        sendMessage(ctx, responseHeader, responseBody);
    }
    
    /**
     * 发送ISUP消息
     */
    private void sendMessage(ChannelHandlerContext ctx, IsupMessageHeader header, byte[] body) {
        try {
            // 设置消息总长度
            header.setMessageLength(IsupMessageHeader.HEADER_LENGTH + body.length);
            
            // 序列化消息头
            byte[] headerBytes = header.toBytes();
            
            // 处理加密（如果需要）
            byte[] encryptedBody = body;
            if (header.getEncryptionFlag() == 0x01) {
                IsupDevice device = sessionManager.getDevice(header.getTargetDeviceId());
                if (device != null && device.getEncryptionKey() != null) {
                    encryptedBody = IsupEncryptionUtil.encrypt(
                        body, 
                        device.getEncryptionKey(), 
                        header.getEncryptionFlag()
                    );
                    // 更新消息长度
                    header.setMessageLength(IsupMessageHeader.HEADER_LENGTH + encryptedBody.length);
                    headerBytes = header.toBytes();
                }
            }
            
            // 计算CRC校验码
            byte[] crcData = new byte[headerBytes.length + encryptedBody.length];
            System.arraycopy(headerBytes, 0, crcData, 0, headerBytes.length);
            System.arraycopy(encryptedBody, 0, crcData, headerBytes.length, encryptedBody.length);
            
            int crc = IsupCrcUtil.calculateCrc16(crcData);
            header.setCrcCode((short) crc);
            headerBytes = header.toBytes(); // 重新序列化包含CRC的头部
            
            // 构建完整的消息
            ByteBuf buffer = ctx.alloc().buffer(headerBytes.length + encryptedBody.length + 2);
            buffer.writeBytes(headerBytes);
            buffer.writeBytes(encryptedBody);
            buffer.writeShort(crc); // 大端模式写入CRC
            
            // 发送消息
            ctx.writeAndFlush(buffer);
            
            log.debug("发送ISUP消息：类型={}, 长度={}", 
                     IsupMessageType.getMessageTypeDescription(header.getMessageType()), 
                     buffer.readableBytes());
            
        } catch (Exception e) {
            log.error("发送ISUP消息失败", e);
        }
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("ISUP客户端连接：{}", ctx.channel().remoteAddress());
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("ISUP客户端断开：{}", ctx.channel().remoteAddress());
        
        // 清理会话
        IsupDevice device = sessionManager.getDeviceByChannel(ctx.channel());
        if (device != null) {
            device.setOnline(false);
            log.info("设备离线：{}", device.getDeviceSerial());
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("ISUP连接异常：{}", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }
}
