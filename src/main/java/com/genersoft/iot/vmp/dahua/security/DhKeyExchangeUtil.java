package com.genersoft.iot.vmp.dahua.security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Diffie-Hellman密钥交换工具类
 * 用于大华协议安全通信的密钥协商
 */
public class DhKeyExchangeUtil {
    
    private static final String ALGORITHM = "DH";
    private static final int KEY_SIZE = 2048; // 使用2048位密钥长度
    
    static {
        // 注册BouncyCastle提供者
        Security.addProvider(new BouncyCastleProvider());
    }
    
    /**
     * 生成DH密钥对
     * @return DH密钥对
     * @throws NoSuchAlgorithmException
     * @throws InvalidAlgorithmParameterException
     */
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(ALGORITHM, "BC");
        keyPairGen.initialize(KEY_SIZE);
        return keyPairGen.generateKeyPair();
    }
    
    /**
     * 从编码的公钥字节恢复PublicKey对象
     * @param encodedPublicKey 编码的公钥字节
     * @return PublicKey对象
     * @throws InvalidKeySpecException
     * @throws NoSuchAlgorithmException
     */
    public static PublicKey getPublicKeyFromBytes(byte[] encodedPublicKey) 
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedPublicKey);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM, "BC");
        return keyFactory.generatePublic(keySpec);
    }
    
    /**
     * 计算共享秘密
     * @param devicePublicKey 设备公钥
     * @param localPrivateKey 本地私钥
     * @return 共享秘密字节数组
     * @throws InvalidKeyException
     */
    public static byte[] computeSharedSecret(PublicKey devicePublicKey, PrivateKey localPrivateKey) 
            throws InvalidKeyException {
        KeyAgreement keyAgreement;
        try {
            keyAgreement = KeyAgreement.getInstance(ALGORITHM, "BC");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("DH algorithm not available", e);
        }
        keyAgreement.init(localPrivateKey);
        keyAgreement.doPhase(devicePublicKey, true);
        return keyAgreement.generateSecret();
    }
    
    /**
     * 将PublicKey编码为字节数组
     * @param publicKey 公钥
     * @return 编码后的字节数组
     */
    public static byte[] encodePublicKey(PublicKey publicKey) {
        return publicKey.getEncoded();
    }
    
    /**
     * 生成Base64编码的公钥字符串（用于日志或调试）
     * @param publicKey 公钥
     * @return Base64编码字符串
     */
    public static String publicKeyToBase64(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }
}
