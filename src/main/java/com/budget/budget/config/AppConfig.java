package com.budget.budget.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
public class AppConfig {

    private static final String ENCRYPTION_ALGORITHM = "AES";
    private static final String SECRET_KEY_STRING = "4h8rL2mN7vXqPzE3";
    // Must be 16 characters
    private static final SecretKeySpec SECRET_KEY = new SecretKeySpec(SECRET_KEY_STRING.getBytes(), ENCRYPTION_ALGORITHM);

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error occurred during encryption", e);
        }
    }

    private String decrypt(String encryptedText) {
        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, SECRET_KEY);
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
            return new String(cipher.doFinal(decodedBytes));
        } catch (Exception e) {
            throw new RuntimeException("Error occurred during decryption", e);
        }
    }

    // Example usage for encrypting/decrypting specific fields like username or amount
    public String encryptUsername(String username) {
        return encrypt(username);
    }

    public String decryptUsername(String encryptedUsername) {
        return decrypt(encryptedUsername);
    }

    public String encryptName(String name) {
        return encrypt(name);
    }

    public String decryptName(String encryptedName) {
        return decrypt(encryptedName);
    }

    public String encryptEmail(String email) {
        return encrypt(email);
    }

    public String decryptEmail(String encryptedEmail) {
        return decrypt(encryptedEmail);
    }

    public String encryptAmount(String amount) {
        return encrypt(amount);
    }

    public String decryptAmount(String encryptedAmount) {
        return decrypt(encryptedAmount);
    }

    public String encryptString(String string) {
        return encrypt(string);
    }

    public String decryptString(String encryptedString) {
        return decrypt(encryptedString);
    }
}
