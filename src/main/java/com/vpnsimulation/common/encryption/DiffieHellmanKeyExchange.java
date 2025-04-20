package com.vpnsimulation.common.encryption;

import javax.crypto.KeyAgreement;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Implements Diffie-Hellman key exchange protocol for secure key sharing between client and server.
 */
public class DiffieHellmanKeyExchange {
    
    private KeyPair keyPair;
    private KeyAgreement keyAgreement;
    private final int KEY_SIZE = 2048;
    
    /**
     * Initializes the Diffie-Hellman key exchange by generating key pairs
     */
    public void init() throws NoSuchAlgorithmException, InvalidKeyException {
        // Generate a key pair
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
        keyPairGenerator.initialize(KEY_SIZE);
        keyPair = keyPairGenerator.generateKeyPair();
        
        // Initialize the key agreement with private key
        keyAgreement = KeyAgreement.getInstance("DH");
        keyAgreement.init(keyPair.getPrivate());
    }
    
    /**
     * Returns the public key encoded as a Base64 string
     */
    public String getPublicKeyEncoded() {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }
    
    /**
     * Process the remote party's public key and generate the shared secret
     */
    public byte[] generateSharedSecret(String remotePublicKeyEncoded) 
            throws Exception {
        // Decode the remote public key
        byte[] remotePublicKeyBytes = Base64.getDecoder().decode(remotePublicKeyEncoded);
        
        // Convert to public key
        KeyFactory keyFactory = KeyFactory.getInstance("DH");
        PublicKey remotePublicKey = keyFactory.generatePublic(
                new X509EncodedKeySpec(remotePublicKeyBytes));
        
        // Generate shared secret
        keyAgreement.doPhase(remotePublicKey, true);
        return keyAgreement.generateSecret();
    }
}
