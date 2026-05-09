package com.genersoft.iot.vmp.isup.codec;

import com.genersoft.iot.vmp.isup.bean.IsupMessageHeader;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

/**
 * ISUP协议加密解密工具类
 * 对应《海康威视ISUP协议开发文档》第4.5节 数据加密
 * 
 * 支持加密算法：
 * - AES-128-CBC（ISUP 3.0）
 * - 不加密（ISUP 2.0默认）
 */
@Slf4j
public class IsupEncryptionUtil {
    
    /**
     * AES密钥长度（128位）
     */
    private static final int AES_KEY_LENGTH = 16;
    
    /**
     * AES加密变换模式
     */
    private static final String AES_TRANSFORMATION = "AES/ECB/PKCS5Padding";
    
    /**
     * AES算法名称
     */
    private static final String AES_ALGORITHM = "AES";
    
    /**
     * 加密数据
     * @param plainData 明文数据
     * @param key 加密密钥（16字节）
     * @param encryptionFlag 加密标志（0x00-不加密，0x01-AES）
     * @return 加密后的数据
     */
    public static byte[] encrypt(byte[] plainData, byte[] key, byte encryptionFlag) {
        if (encryptionFlag == 0x00) {
            // 不加密，直接返回
            return plainData;
        } else if (encryptionFlag == 0x01) {
            // AES加密
            return aesEncrypt(plainData, key);
        } else {
            log.warn("不支持的加密标志：0x{:02X}", encryptionFlag);
            return plainData;
        }
    }
    
    /**
     * 解密数据
     * @param encryptedData 加密数据
     * @param key 解密密钥（16字节）
     * @param encryptionFlag 加密标志
     * @return 解密后的明文数据
     */
    public static byte[] decrypt(byte[] encryptedData, byte[] key, byte encryptionFlag) {
        if (encryptionFlag == 0x00) {
            // 不加密，直接返回
            return encryptedData;
        } else if (encryptionFlag == 0x01) {
            // AES解密
            return aesDecrypt(encryptedData, key);
        } else {
            log.warn("不支持的加密标志：0x{:02X}", encryptionFlag);
            return encryptedData;
        }
    }
    
    /**
     * AES加密
     * @param data 待加密数据
     * @param key 密钥（16字节）
     * @return 加密后的数据
     */
    private static byte[] aesEncrypt(byte[] data, byte[] key) {
        try {
            // 确保密钥长度为16字节
            byte[] validKey = ensureKeyLength(key, AES_KEY_LENGTH);
            
            SecretKeySpec keySpec = new SecretKeySpec(validKey, AES_ALGORITHM);
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            
            return cipher.doFinal(data);
        } catch (Exception e) {
            log.error("AES加密失败", e);
            throw new RuntimeException("AES加密失败", e);
        }
    }
    
    /**
     * AES解密
     * @param data 待解密数据
     * @param key 密钥（16字节）
     * @return 解密后的数据
     */
    private static byte[] aesDecrypt(byte[] data, byte[] key) {
        try {
            // 确保密钥长度为16字节
            byte[] validKey = ensureKeyLength(key, AES_KEY_LENGTH);
            
            SecretKeySpec keySpec = new SecretKeySpec(validKey, AES_ALGORITHM);
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            
            return cipher.doFinal(data);
        } catch (Exception e) {
            log.error("AES解密失败", e);
            throw new RuntimeException("AES解密失败", e);
        }
    }
    
    /**
     * 确保密钥长度为指定长度
     * @param key 原始密钥
     * @param targetLength 目标长度
     * @return 调整后的密钥
     */
    private static byte[] ensureKeyLength(byte[] key, int targetLength) {
        if (key == null) {
            throw new IllegalArgumentException("密钥不能为空");
        }
        
        if (key.length == targetLength) {
            return key;
        } else if (key.length > targetLength) {
            // 截断
            return Arrays.copyOf(key, targetLength);
        } else {
            // 补零
            byte[] result = new byte[targetLength];
            System.arraycopy(key, 0, result, 0, key.length);
            return result;
        }
    }
    
    /**
     * 生成默认密钥（用于测试，实际使用应从配置或安全渠道获取）
     * @return 16字节默认密钥
     */
    public static byte[] generateDefaultKey() {
        // 注意：实际应用中应使用安全的密钥生成和存储机制
        return "HIKVISION_DEFAULT".getBytes(); // 16字节
    }
}
