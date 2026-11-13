package com.sun.aivideo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/** 主密钥通过部署环境注入，禁止保存在数据库或仓库。nonce 与密文一并编码。 */
@Component
public class CredentialCipher {
    private final SecretKeySpec key;
    private final String keyId;
    private final SecureRandom random = new SecureRandom();

    public CredentialCipher(@Value("${aivideo.credentials.master-key:}") String encoded,
                            @Value("${aivideo.credentials.key-id:v1}") String keyId) {
        try {
            byte[] bytes = Base64.getDecoder().decode(encoded);
            if (bytes.length != 32 || !keyId.matches("[A-Za-z0-9._-]{1,64}")) throw new IllegalArgumentException();
            this.key = new SecretKeySpec(bytes, "AES");
            this.keyId = keyId;
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("请配置 AIVIDEO_CREDENTIAL_MASTER_KEY（Base64 编码的 32 字节随机密钥）");
        }
    }
    public String keyId() { return keyId; }

    public String encrypt(String value, String owner) {
        try {
            byte[] nonce = new byte[12]; random.nextBytes(nonce);
            Cipher cipher = cipher(Cipher.ENCRYPT_MODE, nonce, owner);
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] result = Arrays.copyOf(nonce, nonce.length + encrypted.length);
            System.arraycopy(encrypted, 0, result, nonce.length, encrypted.length);
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) { throw new IllegalStateException("密钥加密失败"); }
    }

    public String decrypt(String encoded, String owner, String version) {
        try {
            if (!keyId.equals(version)) throw new IllegalArgumentException();
            byte[] data = Base64.getDecoder().decode(encoded);
            if (data.length < 29) throw new IllegalArgumentException();
            return new String(cipher(Cipher.DECRYPT_MODE, Arrays.copyOf(data, 12), owner)
                    .doFinal(data, 12, data.length - 12), StandardCharsets.UTF_8);
        } catch (Exception e) { throw new IllegalStateException("无法解密任务凭证，请检查加密主密钥配置"); }
    }

    private Cipher cipher(int mode, byte[] nonce, String owner) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, key, new GCMParameterSpec(128, nonce));
        cipher.updateAAD(owner.getBytes(StandardCharsets.UTF_8));
        return cipher;
    }
}
