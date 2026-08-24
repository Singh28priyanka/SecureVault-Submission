package com.example.securevault.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class AESUtil {

    private final String secretKey;

    public AESUtil(@Value("${aes.secret.key}") String secretKey) {

        if (secretKey == null || secretKey.length() != 16) {
            throw new IllegalArgumentException(
                    "AES secret key must contain exactly 16 characters"
            );
        }

        this.secretKey = secretKey;
    }

    public String encrypt(String data) {
        try {
            SecretKeySpec key = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8),
                    "AES"
            );

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] encryptedBytes = cipher.doFinal(
                    data.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getEncoder()
                    .encodeToString(encryptedBytes);

        } catch (Exception exception) {
            throw new RuntimeException(
                    "Error while encrypting password",
                    exception
            );
        }
    }

    public String decrypt(String encryptedData) {
        try {
            SecretKeySpec key = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8),
                    "AES"
            );

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);

            byte[] encryptedBytes = Base64.getDecoder()
                    .decode(encryptedData);

            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            return new String(
                    decryptedBytes,
                    StandardCharsets.UTF_8
            );

        } catch (Exception exception) {
            throw new RuntimeException(
                    "Error while decrypting password",
                    exception
            );
        }
    }
}