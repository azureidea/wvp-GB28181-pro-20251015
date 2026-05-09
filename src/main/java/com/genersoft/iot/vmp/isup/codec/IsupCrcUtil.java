package com.genersoft.iot.vmp.isup.codec;

/**
 * ISUP协议CRC16校验工具类
 * 对应《海康威视ISUP协议开发文档》第4.4节 数据校验
 * 
 * 校验算法：CRC-16/MODBUS
 * 多项式：0x8005（反射：0xA001）
 * 初始值：0xFFFF
 * 结果异或：0x0000
 * 输入反转：true
 * 输出反转：true
 */
public class IsupCrcUtil {
    
    /**
     * CRC16查找表（预计算，提高效率）
     */
    private static final int[] CRC16_TABLE = new int[256];
    
    static {
        // 初始化CRC16查找表
        for (int i = 0; i < 256; i++) {
            int crc = i;
            for (int j = 0; j < 8; j++) {
                if ((crc & 1) != 0) {
                    crc = (crc >>> 1) ^ 0xA001;
                } else {
                    crc = crc >>> 1;
                }
            }
            CRC16_TABLE[i] = crc;
        }
    }
    
    /**
     * 计算CRC16校验码
     * @param data 数据字节数组
     * @param offset 起始偏移量
     * @param length 数据长度
     * @return CRC16校验码（2字节，无符号）
     */
    public static int calculateCrc16(byte[] data, int offset, int length) {
        if (data == null || data.length < offset + length) {
            throw new IllegalArgumentException("数据无效");
        }
        
        int crc = 0xFFFF;
        for (int i = offset; i < offset + length; i++) {
            int index = (crc ^ (data[i] & 0xFF)) & 0xFF;
            crc = (crc >>> 8) ^ CRC16_TABLE[index];
        }
        
        return crc & 0xFFFF;
    }
    
    /**
     * 计算整个数组的CRC16校验码
     * @param data 数据字节数组
     * @return CRC16校验码
     */
    public static int calculateCrc16(byte[] data) {
        return calculateCrc16(data, 0, data.length);
    }
    
    /**
     * 验证CRC16校验码是否正确
     * @param data 包含校验码的完整数据
     * @param offset 数据起始偏移量
     * @param length 数据长度（包含2字节校验码）
     * @return true-校验通过，false-校验失败
     */
    public static boolean verifyCrc16(byte[] data, int offset, int length) {
        if (length < 2) {
            return false;
        }
        
        // 提取接收到的校验码（大端模式）
        int receivedCrc = ((data[offset + length - 2] & 0xFF) << 8) | 
                          (data[offset + length - 1] & 0xFF);
        
        // 计算数据的CRC16（不包括最后2字节校验码）
        int calculatedCrc = calculateCrc16(data, offset, length - 2);
        
        return receivedCrc == calculatedCrc;
    }
    
    /**
     * 将CRC16校验码转换为字节数组（大端模式）
     * @param crc CRC16校验码
     * @return 2字节数组
     */
    public static byte[] crcToBytes(int crc) {
        return new byte[] {
            (byte) ((crc >> 8) & 0xFF),
            (byte) (crc & 0xFF)
        };
    }
}
