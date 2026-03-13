package ro.app.client.service;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.util.Base64;

@Service
public class EncryptionService {
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 12;

    //Deriva cheie AES-256 din parola + salt via PBKDF2
    public SecretKey deriveKey(String password, byte[] salt) throws Exception{
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec= new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    //Cripteaza: returneaza un string care contine salt + IV + textul criptat, separate de ':'
    public String encrypt(String plaintext, String password) throws Exception{
        byte[] salt = generateRandom(SALT_LENGTH);
        byte[] iv = generateRandom(IV_LENGTH);
        SecretKey key = deriveKey(password, salt);

        Cipher cipher = Cipher.getInstance(("AES/GCM/NoPadding"));
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        String saltStr = Base64.getEncoder().encodeToString(salt);
        String ivStr = Base64.getEncoder().encodeToString(iv);
        String ciphertextStr = Base64.getEncoder().encodeToString(ciphertext);
        return saltStr + ":" + ivStr + ":" + ciphertextStr;
    }

    //Decripteaza: primeste un string format din salt + IV + text criptat, returneaza textul original
    public String decrypt(String encryptedData, String password) throws Exception{
        String[] parts = encryptedData.split(":");
        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] iv = Base64.getDecoder().decode(parts[1]);
        byte[] ciphertext = Base64.getDecoder().decode(parts[2]);

        SecretKey key = deriveKey(password, salt);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] plaintextBytes = cipher.doFinal(ciphertext);
        return new String(plaintextBytes, StandardCharsets.UTF_8);
    }

    private byte[] generateRandom(int length) {
        byte[] bytes = new byte[length];
        new java.security.SecureRandom().nextBytes(bytes);
        return bytes;
    }
}
