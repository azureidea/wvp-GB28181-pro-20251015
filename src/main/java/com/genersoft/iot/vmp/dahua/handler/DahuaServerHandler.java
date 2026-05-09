package com.genersoft.iot.vmp.dahua.handler;

import com.genersoft.iot.vmp.dahua.bean.*;
import com.genersoft.iot.vmp.dahua.security.AesCryptoUtil;
import com.genersoft.iot.vmp.dahua.security.DhKeyExchangeUtil;
import com.genersoft.iot.vmp.dahua.service.DahuaService;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 大华设备协议处理器
 * 支持ISUP类似的大华私有TCP协议，包含加密、心跳、信令解析等功能
 */
@Slf4j
public class DahuaServerHandler extends SimpleChannelInboundHandler<ByteBuf> {
    
    // 会话管理
    private static final Map<String, DahuaSessionContext> sessions = new ConcurrentHashMap<>();
    private static final Map<String, KeyPair> sessionKeyPairs = new ConcurrentHashMap<>();
    
    // 服务层引用
    private final DahuaService dahuaService;
    
    // 连接状态枚举
    private enum ConnectionState {
        CONNECTED,      // 已连接
        WAIT_REGISTRATION,  // 等待注册
        AUTHENTICATING,     // 认证中（DH密钥交换）
        SECURING,           // 加密建立中
        AUTHENTICATED,      // 已认证
        READY               // 就绪（可传输业务数据）
    }
    
    // 每个通道的状态
    private ConnectionState state = ConnectionState.CONNECTED;
    private String currentSessionId;
    
    public DahuaServerHandler(DahuaService dahuaService) {
        this.dahuaService = dahuaService;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("Dahua device connected: {}", ctx.channel().remoteAddress());
        state = ConnectionState.WAIT_REGISTRATION;
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        if (!msg.isReadable()) {
            return;
        }
        
        // 读取消息头（大端模式）
        msg.markReaderIndex();
        if (msg.readableBytes() < 12) {
            return; // 消息不完整
        }
        
        // 大华协议头格式（示例）：
        // [Magic:2][Version:2][CmdType:4][Seq:4][Length:4][Payload...]
        msg.order(ByteOrder.BIG_ENDIAN);
        
        short magic = msg.readShort();
        short version = msg.readShort();
        int cmdType = msg.readInt();
        int seq = msg.readInt();
        int length = msg.readInt();
        
        // 验证魔数（示例：0xDADA）
        if (magic != 0xDADA) {
            log.warn("Invalid magic number: 0x{:04X}", magic);
            ctx.close();
            return;
        }
        
        // 检查消息完整性
        if (msg.readableBytes() < length) {
            msg.resetReaderIndex();
            return; // 等待更多数据
        }
        
        // 读取负载数据
        byte[] payload = new byte[length];
        msg.readBytes(payload);
        
        // 根据连接状态处理消息
        switch (state) {
            case WAIT_REGISTRATION:
                handleRegistration(ctx, cmdType, seq, payload, version);
                break;
                
            case AUTHENTICATING:
                handleKeyExchange(ctx, cmdType, seq, payload);
                break;
                
            case SECURING:
            case AUTHENTICATED:
            case READY:
                handleBusinessMessage(ctx, cmdType, seq, payload);
                break;
                
            default:
                log.warn("Unknown state: {}", state);
        }
    }
    
    /**
     * 处理设备注册请求
     */
    private void handleRegistration(ChannelHandlerContext ctx, int cmdType, int seq, 
                                     byte[] payload, short version) {
        if (cmdType != 0x0001) { // 假设0x0001是注册命令
            log.warn("Expected registration command, got: 0x{:08X}", cmdType);
            return;
        }
        
        try {
            // 解析注册包（设备ID、用户名、密码等）
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            buffer.order(ByteOrder.BIG_ENDIAN);
            
            // 读取设备ID（变长字符串，前2字节为长度）
            short deviceIdLen = buffer.getShort();
            byte[] deviceIdBytes = new byte[deviceIdLen];
            buffer.get(deviceIdBytes);
            String deviceId = new String(deviceIdBytes, "UTF-8");
            
            // 读取用户名
            short usernameLen = buffer.getShort();
            byte[] usernameBytes = new byte[usernameLen];
            buffer.get(usernameBytes);
            String username = new String(usernameBytes, "UTF-8");
            
            // 读取密码（可能是加密的）
            short passwordLen = buffer.getShort();
            byte[] passwordBytes = new byte[passwordLen];
            buffer.get(passwordBytes);
            String password = new String(passwordBytes, "UTF-8");
            
            log.info("Device registration request: deviceId={}, username={}", deviceId, username);
            
            // 验证设备凭证
            boolean authenticated = dahuaService.authenticateDevice(deviceId, username, password);
            
            if (authenticated) {
                // 生成会话ID
                String sessionId = generateSessionId(deviceId);
                currentSessionId = sessionId;
                
                // 创建会话上下文
                DahuaSessionContext sessionCtx = new DahuaSessionContext();
                sessionCtx.setDeviceId(deviceId);
                sessionCtx.setSessionId(sessionId);
                sessionCtx.setProtocolVersion(version);
                sessionCtx.setDeviceIp(ctx.channel().remoteAddress().toString());
                
                // 检查是否支持加密
                boolean supportEncryption = (buffer.remaining() > 0) && (buffer.get() == 0x01);
                sessionCtx.setSupportEncryption(supportEncryption);
                
                sessions.put(sessionId, sessionCtx);
                
                if (supportEncryption) {
                    // 启动DH密钥交换
                    state = ConnectionState.AUTHENTICATING;
                    initiateKeyExchange(ctx, sessionId, seq);
                } else {
                    // 不加密，直接完成注册
                    sessionCtx.setAuthenticated(true);
                    sessionCtx.setEncrypted(false);
                    state = ConnectionState.READY;
                    sendRegistrationResponse(ctx, sessionId, seq, true, false);
                    
                    // 通知服务层设备上线
                    dahuaService.onDeviceOnline(sessionCtx);
                }
            } else {
                // 认证失败
                sendRegistrationResponse(ctx, null, seq, false, false);
                log.warn("Device authentication failed: {}", deviceId);
                ctx.close();
            }
            
        } catch (Exception e) {
            log.error("Error processing registration", e);
            sendRegistrationResponse(ctx, null, seq, false, false);
            ctx.close();
        }
    }
    
    /**
     * 发起DH密钥交换
     */
    private void initiateKeyExchange(ChannelHandlerContext ctx, String sessionId, int reqSeq) {
        try {
            // 生成DH密钥对
            KeyPair keyPair = DhKeyExchangeUtil.generateKeyPair();
            sessionKeyPairs.put(sessionId, keyPair);
            
            // 构造密钥交换请求
            byte[] localPublicKey = DhKeyExchangeUtil.encodePublicKey(keyPair.getPublic());
            
            // 发送公钥给设备
            ByteBuf response = buildKeyExchangeRequest(localPublicKey, reqSeq);
            ctx.writeAndFlush(response);
            
            log.info("DH key exchange initiated for session: {}", sessionId);
            
        } catch (Exception e) {
            log.error("Failed to initiate key exchange", e);
            sendRegistrationResponse(ctx, sessionId, reqSeq, false, false);
        }
    }
    
    /**
     * 处理密钥交换响应
     */
    private void handleKeyExchange(ChannelHandlerContext ctx, int cmdType, int seq, byte[] payload) {
        if (cmdType != 0x0002) { // 假设0x0002是密钥交换响应
            log.warn("Expected key exchange response, got: 0x{:08X}", cmdType);
            return;
        }
        
        try {
            DahuaSessionContext sessionCtx = sessions.get(currentSessionId);
            if (sessionCtx == null) {
                log.error("Session not found: {}", currentSessionId);
                return;
            }
            
            // 解析设备公钥
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            buffer.order(ByteOrder.BIG_ENDIAN);
            short pubKeyLen = buffer.getShort();
            byte[] devicePubKeyBytes = new byte[pubKeyLen];
            buffer.get(devicePubKeyBytes);
            
            // 恢复公钥对象
            PublicKey devicePublicKey = DhKeyExchangeUtil.getPublicKeyFromBytes(devicePubKeyBytes);
            
            // 获取本地私钥
            KeyPair keyPair = sessionKeyPairs.get(currentSessionId);
            if (keyPair == null) {
                log.error("KeyPair not found for session: {}", currentSessionId);
                return;
            }
            
            // 计算共享秘密
            byte[] sharedSecret = DhKeyExchangeUtil.computeSharedSecret(
                devicePublicKey, keyPair.getPrivate());
            
            // 派生AES密钥
            byte[] aesKey = AesCryptoUtil.deriveAesKey(sharedSecret, 32); // 256位
            byte[] aesIv = AesCryptoUtil.generateIv();
            
            // 保存密钥到会话上下文
            sessionCtx.setDeviceDhPublicKey(devicePubKeyBytes);
            sessionCtx.setSharedSecret(sharedSecret);
            sessionCtx.setAesKey(aesKey);
            sessionCtx.setAesIv(aesIv);
            sessionCtx.setEncrypted(true);
            sessionCtx.setAuthenticated(true);
            
            // 清理临时密钥对
            sessionKeyPairs.remove(currentSessionId);
            
            // 更新状态
            state = ConnectionState.READY;
            
            // 发送加密确认
            sendEncryptionConfirm(ctx, currentSessionId, seq);
            
            // 通知服务层设备上线（加密模式）
            dahuaService.onDeviceOnline(sessionCtx);
            
            log.info("DH key exchange completed, encryption enabled for session: {}", currentSessionId);
            
        } catch (Exception e) {
            log.error("Key exchange failed", e);
            ctx.close();
        }
    }
    
    /**
     * 处理业务消息（预览、云台、报警等）
     */
    private void handleBusinessMessage(ChannelHandlerContext ctx, int cmdType, int seq, byte[] payload) {
        try {
            DahuaSessionContext sessionCtx = sessions.get(currentSessionId);
            if (sessionCtx == null) {
                log.error("Session not found: {}", currentSessionId);
                return;
            }
            
            // 如果启用了加密，先解密
            byte[] decryptedPayload = payload;
            if (sessionCtx.isEncrypted()) {
                // 假设前16字节是IV，后面是密文
                if (payload.length > 16) {
                    byte[] iv = new byte[16];
                    System.arraycopy(payload, 0, iv, 0, 16);
                    byte[] ciphertext = new byte[payload.length - 16];
                    System.arraycopy(payload, 16, ciphertext, 0, ciphertext.length);
                    decryptedPayload = AesCryptoUtil.decrypt(ciphertext, sessionCtx.getAesKey(), iv);
                }
            }
            
            // 根据命令类型分发处理
            switch (cmdType) {
                case 0x0100: // 心跳
                    handleHeartbeat(ctx, seq, sessionCtx);
                    break;
                    
                case 0x0200: // 资源列表请求
                    dahuaService.handleResourceListRequest(sessionCtx, seq, decryptedPayload);
                    break;
                    
                case 0x0300: // 实时预览请求
                    dahuaService.handlePreviewRequest(sessionCtx, seq, decryptedPayload);
                    break;
                    
                case 0x0310: // 云台控制
                    dahuaService.handlePtzControl(sessionCtx, seq, decryptedPayload);
                    break;
                    
                case 0x0320: // 预置位控制
                    dahuaService.handlePresetControl(sessionCtx, seq, decryptedPayload);
                    break;
                    
                case 0x0400: // 录像查询
                    dahuaService.handleRecordQuery(sessionCtx, seq, decryptedPayload);
                    break;
                    
                case 0x0410: // 录像回放
                    dahuaService.handleRecordPlayback(sessionCtx, seq, decryptedPayload);
                    break;
                    
                case 0x0500: // 报警上报
                    dahuaService.handleAlarmReport(sessionCtx, seq, decryptedPayload);
                    break;
                    
                case 0x0600: // 对讲开始
                    dahuaService.handleIntercomStart(sessionCtx, seq, decryptedPayload);
                    break;
                    
                case 0x0610: // 对讲音频数据
                    dahuaService.handleIntercomAudio(sessionCtx, seq, decryptedPayload);
                    break;
                    
                case 0x0700: // 设备配置
                    dahuaService.handleDeviceConfig(sessionCtx, seq, decryptedPayload);
                    break;
                    
                default:
                    log.warn("Unknown command: 0x{:08X}", cmdType);
            }
            
        } catch (Exception e) {
            log.error("Error processing business message", e);
        }
    }
    
    /**
     * 处理心跳
     */
    private void handleHeartbeat(ChannelHandlerContext ctx, int seq, DahuaSessionContext sessionCtx) {
        sessionCtx.updateHeartbeat();
        
        // 回复心跳确认
        ByteBuf response = buildResponse(0x0101, seq, new byte[]{0x00});
        ctx.writeAndFlush(response);
        
        log.debug("Heartbeat received from device: {}", sessionCtx.getDeviceId());
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (currentSessionId != null) {
            DahuaSessionContext sessionCtx = sessions.remove(currentSessionId);
            if (sessionCtx != null) {
                dahuaService.onDeviceOffline(sessionCtx);
                log.info("Device disconnected: {}", sessionCtx.getDeviceId());
            }
            sessionKeyPairs.remove(currentSessionId);
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Exception in Dahua handler", cause);
        ctx.close();
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 生成会话ID
     */
    private String generateSessionId(String deviceId) {
        return deviceId + "_" + System.currentTimeMillis() + "_" + 
               (int)(Math.random() * 10000);
    }
    
    /**
     * 构建注册响应
     */
    private ByteBuf sendRegistrationResponse(ChannelHandlerContext ctx, String sessionId, 
                                              int seq, boolean success, boolean encrypted) {
        ByteBuffer buffer = ByteBuffer.allocate(256);
        buffer.order(ByteOrder.BIG_ENDIAN);
        
        if (success) {
            buffer.put((byte) 0x00); // 成功
            if (sessionId != null) {
                byte[] sessionIdBytes = sessionId.getBytes();
                buffer.putShort((short) sessionIdBytes.length);
                buffer.put(sessionIdBytes);
            }
            buffer.put(encrypted ? (byte) 0x01 : (byte) 0x00);
        } else {
            buffer.put((byte) 0x01); // 失败
            buffer.putShort((short) 0);
        }
        
        int payloadLen = buffer.position();
        buffer.flip();
        byte[] payload = new byte[payloadLen];
        buffer.get(payload);
        
        ByteBuf response = buildResponse(0x0001, seq, payload);
        ctx.writeAndFlush(response);
        return response;
    }
    
    /**
     * 构建密钥交换请求
     */
    private ByteBuf buildKeyExchangeRequest(byte[] publicKey, int seq) {
        ByteBuffer buffer = ByteBuffer.allocate(publicKey.length + 10);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) publicKey.length);
        buffer.put(publicKey);
        
        int payloadLen = buffer.position();
        buffer.flip();
        byte[] payload = new byte[payloadLen];
        buffer.get(payload);
        
        return buildResponse(0x0002, seq, payload);
    }
    
    /**
     * 构建加密确认
     */
    private void sendEncryptionConfirm(ChannelHandlerContext ctx, String sessionId, int seq) {
        byte[] payload = sessionId.getBytes();
        ByteBuf response = buildResponse(0x0003, seq, payload);
        ctx.writeAndFlush(response);
    }
    
    /**
     * 构建通用响应包
     */
    private ByteBuf buildResponse(int cmdType, int seq, byte[] payload) {
        ByteBuf buffer = io.netty.buffer.Unpooled.buffer(20 + payload.length);
        buffer.order(ByteOrder.BIG_ENDIAN);
        
        buffer.writeShort(0xDADA); // Magic
        buffer.writeShort(0x0100); // Version
        buffer.writeInt(cmdType);
        buffer.writeInt(seq);
        buffer.writeInt(payload.length);
        buffer.writeBytes(payload);
        
        // TODO: 添加CRC校验
        
        return buffer;
    }
}
