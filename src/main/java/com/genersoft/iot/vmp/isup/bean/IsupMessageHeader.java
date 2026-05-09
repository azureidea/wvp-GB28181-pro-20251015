package com.genersoft.iot.vmp.isup.bean;

import lombok.Data;

/**
 * ISUP协议消息头结构
 * 对应《海康威视ISUP协议开发文档》第4.2节 消息头格式
 * 
 * 消息头总长度：32字节（固定）
 * 字节序：大端模式（Big-Endian）
 */
@Data
public class IsupMessageHeader {
    
    /**
     * 消息头长度（固定为32字节）
     */
    public static final int HEADER_LENGTH = 32;
    
    /**
     * 起始标识（2字节）：固定为0x687A
     */
    private short startFlag = 0x687A;
    
    /**
     * 消息长度（4字节）：整个消息的长度（包含消息头和消息体），不包括校验码
     */
    private int messageLength;
    
    /**
     * 协议版本（1字节）：0x01-ISUP 2.0, 0x02-ISUP 3.0
     */
    private byte protocolVersion;
    
    /**
     * 保留位（1字节）：固定为0x00
     */
    private byte reserved = 0x00;
    
    /**
     * 消息类型（2字节）：见IsupMessageType定义
     */
    private short messageType;
    
    /**
     * 序列号（4字节）：消息的唯一标识，由发送方生成，响应消息需使用相同序列号
     */
    private int sequenceNumber;
    
    /**
     * 源设备ID（8字节）：发送方的设备序列号（ASCII编码，不足补0x00）
     */
    private String sourceDeviceId;
    
    /**
     * 目标设备ID（8字节）：接收方的设备序列号（ASCII编码，不足补0x00）
     */
    private String targetDeviceId;
    
    /**
     * 加密标志（1字节）：0x00-不加密，0x01-AES加密，0x02-RSA加密
     */
    private byte encryptionFlag;
    
    /**
     * 保留位2（3字节）：固定为0x000000
     */
    private int reserved2 = 0x000000;
    
    /**
     * 校验码（2字节）：CRC16校验（从消息头开始到消息体结束，不包括校验码本身）
     */
    private short crcCode;
    
    /**
     * 将消息头序列化为字节数组（大端模式）
     * @return 32字节的消息头数组
     */
    public byte[] toBytes() {
        byte[] buffer = new byte[HEADER_LENGTH];
        int offset = 0;
        
        // 起始标识（2字节，大端）
        buffer[offset++] = (byte) ((startFlag >> 8) & 0xFF);
        buffer[offset++] = (byte) (startFlag & 0xFF);
        
        // 消息长度（4字节，大端）
        buffer[offset++] = (byte) ((messageLength >> 24) & 0xFF);
        buffer[offset++] = (byte) ((messageLength >> 16) & 0xFF);
        buffer[offset++] = (byte) ((messageLength >> 8) & 0xFF);
        buffer[offset++] = (byte) (messageLength & 0xFF);
        
        // 协议版本（1字节）
        buffer[offset++] = protocolVersion;
        
        // 保留位（1字节）
        buffer[offset++] = reserved;
        
        // 消息类型（2字节，大端）
        buffer[offset++] = (byte) ((messageType >> 8) & 0xFF);
        buffer[offset++] = (byte) (messageType & 0xFF);
        
        // 序列号（4字节，大端）
        buffer[offset++] = (byte) ((sequenceNumber >> 24) & 0xFF);
        buffer[offset++] = (byte) ((sequenceNumber >> 16) & 0xFF);
        buffer[offset++] = (byte) ((sequenceNumber >> 8) & 0xFF);
        buffer[offset++] = (byte) (sequenceNumber & 0xFF);
        
        // 源设备ID（8字节，ASCII编码，不足补0）
        byte[] sourceBytes = sourceDeviceId != null ? sourceDeviceId.getBytes() : new byte[0];
        for (int i = 0; i < 8; i++) {
            buffer[offset++] = i < sourceBytes.length ? sourceBytes[i] : 0x00;
        }
        
        // 目标设备ID（8字节，ASCII编码，不足补0）
        byte[] targetBytes = targetDeviceId != null ? targetDeviceId.getBytes() : new byte[0];
        for (int i = 0; i < 8; i++) {
            buffer[offset++] = i < targetBytes.length ? targetBytes[i] : 0x00;
        }
        
        // 加密标志（1字节）
        buffer[offset++] = encryptionFlag;
        
        // 保留位2（3字节）
        buffer[offset++] = (byte) ((reserved2 >> 16) & 0xFF);
        buffer[offset++] = (byte) ((reserved2 >> 8) & 0xFF);
        buffer[offset++] = (byte) (reserved2 & 0xFF);
        
        // CRC校验码（2字节，大端）- 待计算
        buffer[offset++] = (byte) ((crcCode >> 8) & 0xFF);
        buffer[offset++] = (byte) (crcCode & 0xFF);
        
        return buffer;
    }
    
    /**
     * 从字节数组解析消息头（大端模式）
     * @param data 字节数组
     * @param offset 起始偏移量
     * @return IsupMessageHeader对象
     */
    public static IsupMessageHeader fromBytes(byte[] data, int offset) {
        if (data == null || data.length < offset + HEADER_LENGTH) {
            throw new IllegalArgumentException("数据长度不足");
        }
        
        IsupMessageHeader header = new IsupMessageHeader();
        int pos = offset;
        
        // 起始标识（2字节，大端）
        header.startFlag = (short) (((data[pos++] & 0xFF) << 8) | (data[pos++] & 0xFF));
        
        // 消息长度（4字节，大端）
        header.messageLength = ((data[pos++] & 0xFF) << 24) |
                               ((data[pos++] & 0xFF) << 16) |
                               ((data[pos++] & 0xFF) << 8) |
                               (data[pos++] & 0xFF);
        
        // 协议版本（1字节）
        header.protocolVersion = data[pos++];
        
        // 保留位（1字节）
        header.reserved = data[pos++];
        
        // 消息类型（2字节，大端）
        header.messageType = (short) (((data[pos++] & 0xFF) << 8) | (data[pos++] & 0xFF));
        
        // 序列号（4字节，大端）
        header.sequenceNumber = ((data[pos++] & 0xFF) << 24) |
                                ((data[pos++] & 0xFF) << 16) |
                                ((data[pos++] & 0xFF) << 8) |
                                (data[pos++] & 0xFF);
        
        // 源设备ID（8字节）
        byte[] sourceBytes = new byte[8];
        System.arraycopy(data, pos, sourceBytes, 0, 8);
        header.sourceDeviceId = new String(sourceBytes).replaceAll("\\x00+$", "");
        pos += 8;
        
        // 目标设备ID（8字节）
        byte[] targetBytes = new byte[8];
        System.arraycopy(data, pos, targetBytes, 0, 8);
        header.targetDeviceId = new String(targetBytes).replaceAll("\\x00+$", "");
        pos += 8;
        
        // 加密标志（1字节）
        header.encryptionFlag = data[pos++];
        
        // 保留位2（3字节，大端）
        header.reserved2 = ((data[pos++] & 0xFF) << 16) |
                           ((data[pos++] & 0xFF) << 8) |
                           (data[pos++] & 0xFF);
        
        // CRC校验码（2字节，大端）
        header.crcCode = (short) (((data[pos++] & 0xFF) << 8) | (data[pos++] & 0xFF));
        
        return header;
    }
}
