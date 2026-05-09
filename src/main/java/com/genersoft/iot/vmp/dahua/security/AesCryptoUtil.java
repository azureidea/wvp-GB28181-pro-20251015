package com.genersoft.iot.vmp.dahua.security;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.KDF1BytesGenerator;
import org.bouncycastle.crypto.params.KeyParameter;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * AES加密工具类
 * 用于大华协议加密传输
 */
public class AesCryptoUtil {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    
    /**
     * 通过KDF从共享秘密生成AES密钥
     * @param sharedSecret DH共享秘密
     * @param keyLength 密钥长度（16/24/32字节）
     * @return AES密钥
     */
    public static byte[] deriveAesKey(byte[] sharedSecret, int keyLength) {
        if (sharedSecret == null || sharedSecret.length == 0) {
            throw new IllegalArgumentException("Shared secret cannot be empty");
        }
        
        // 使用KDF1 (ISO-18033-2) 派生密钥
        KDF1BytesGenerator kdf = new KDF1BytesGenerator(new SHA256Digest());
        kdf.init(new KeyParameter(sharedSecret));
        
        byte[] derivedKey = new byte[keyLength];
        kdf.generateBytes(derivedKey, 0, derivedKey.length);
        
        return derivedKey;
    }
    
    /**
     * 生成随机IV
     * @return 16字节IV
     */
    public static byte[] generateIv() {
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[16];
        random.nextBytes(iv);
        return iv;
    }
    
    /**
     * AES-CBC加密
     * @param data 原始数据
     * @param key AES密钥
     * @param iv 初始化向量
     * @return 加密后的数据
     */
    public static byte[] encrypt(byte[] data, byte[] key, byte[] iv) throws Exception {
        if (data == null || data.length == 0) {
            return new byte[0];
        }
        
        SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        
        return cipher.doFinal(data);
    }
    
    /**
     * AES-CBC解密
     * @param encryptedData 加密数据
     * @param key AES密钥
     * @param iv 初始化向量
     * @return 解密后的数据
     */
    public static byte[] decrypt(byte[] encryptedData, byte[] key, byte[] iv) throws Exception {
        if (encryptedData == null || encryptedData.length == 0) {
            return new byte[0];
        }
        
        SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        
        return cipher.doFinal(encryptedData);
    }
    
    /**
     * 将字节数组转换为Hex字符串
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    /**
     * 将Hex字符串转换为字节数组
     */
    public static byte[] hexToBytes(String hex) {
        if (hex == null || hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Invalid hex string");
        }
        
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            bytes[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return bytes;
    }
}
