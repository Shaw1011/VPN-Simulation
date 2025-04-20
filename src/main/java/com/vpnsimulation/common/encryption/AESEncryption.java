package com.vpnsimulation.common.encryption;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Handles AES encryption and decryption of data
 */
public class AESEncryption {
    
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    
    private SecretKey secretKey;
    
    /**
     * Initialize encryption with a shared secret from Diffie-Hellman exchange
     */
    public void initFromSharedSecret(byte[] sharedSecret) {
        // Use first 32 bytes (256 bits) of the shared secret as AES key
        byte[] keyBytes = new byte[32];
        System.arraycopy(sharedSecret, 0, keyBytes, 0, Math.min(sharedSecret.length, keyBytes.length));
        secretKey = new SecretKeySpec(keyBytes, "AES");
    }
    
    /**
     * Encrypts plaintext data using AES-GCM
     * @param plaintext Data to encrypt
     * @return Base64 encoded string containing IV and ciphertext
     */
    public String encrypt(String plaintext) throws Exception {
        // Generate random IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        
        // Initialize cipher for encryption
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
        
        // Encrypt the data
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes());
        
        // Combine IV and ciphertext and return Base64 encoded
        byte[] encryptedData = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, encryptedData, 0, iv.length);
        System.arraycopy(ciphertext, 0, encryptedData, iv.length, ciphertext.length);
        
        return Base64.getEncoder().encodeToString(encryptedData);
    }
    
    /**
     * Decrypts AES-GCM encrypted data
     * @param encryptedData Base64 encoded string containing IV and ciphertext
     * @return Decrypted plaintext
     */
    public String decrypt(String encryptedData) throws Exception {
        // Decode from Base64
        byte[] decodedData = Base64.getDecoder().decode(encryptedData);
        
        // Extract IV and ciphertext
        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] ciphertext = new byte[decodedData.length - GCM_IV_LENGTH];
        
        System.arraycopy(decodedData, 0, iv, 0, iv.length);
        System.arraycopy(decodedData, iv.length, ciphertext, 0, ciphertext.length);
        
        // Initialize cipher for decryption
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
        
        // Decrypt and return as string
        byte[] decryptedData = cipher.doFinal(ciphertext);
        return new String(decryptedData);
    }
    
    /**
     * Encrypts binary data using AES-GCM
     * @param data Data to encrypt
     * @return Base64 encoded string containing IV and ciphertext
     */
    public String encryptBytes(byte[] data) throws Exception {
        // Generate random IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        
        // Initialize cipher for encryption
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
        
        // Encrypt the data
        byte[] ciphertext = cipher.doFinal(data);
        
        // Combine IV and ciphertext and return Base64 encoded
        byte[] encryptedData = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, encryptedData, 0, iv.length);
        System.arraycopy(ciphertext, 0, encryptedData, iv.length, ciphertext.length);
        
        return Base64.getEncoder().encodeToString(encryptedData);
    }
    
    /**
     * Decrypts AES-GCM encrypted binary data
     * @param encryptedData Base64 encoded string containing IV and ciphertext
     * @return Decrypted binary data
     */
    public byte[] decryptToBytes(String encryptedData) throws Exception {
        // Decode from Base64
        byte[] decodedData = Base64.getDecoder().decode(encryptedData);
        
        // Extract IV and ciphertext
        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] ciphertext = new byte[decodedData.length - GCM_IV_LENGTH];
        
        System.arraycopy(decodedData, 0, iv, 0, iv.length);
        System.arraycopy(decodedData, iv.length, ciphertext, 0, ciphertext.length);
        
        // Initialize cipher for decryption
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
        
        // Decrypt and return as bytes
        return cipher.doFinal(ciphertext);
    }
}
