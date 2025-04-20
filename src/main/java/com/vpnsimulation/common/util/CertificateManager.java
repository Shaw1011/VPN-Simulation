package com.vpnsimulation.common.util;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;

/**
 * Manages X.509 certificates for client and server authentication
 */
public class CertificateManager {

    static {
        // Add Bouncy Castle as a security provider
        Security.addProvider(new BouncyCastleProvider());
    }

    private static final long VALIDITY_PERIOD = 365 * 24 * 60 * 60 * 1000L; // 1 year in milliseconds
    
    private KeyPair keyPair;
    private X509Certificate certificate;
    
    /**
     * Generates a self-signed X.509 certificate
     */
    public void generateSelfSignedCertificate(String cn) throws Exception {
        // Generate a key pair
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        keyPair = keyPairGenerator.generateKeyPair();
        
        // Define certificate details
        X500Name issuer = new X500Name("CN=" + cn);
        X500Name subject = new X500Name("CN=" + cn);
        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());
        Date notBefore = new Date();
        Date notAfter = new Date(System.currentTimeMillis() + VALIDITY_PERIOD);
        
        // Build the certificate
        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer, serialNumber, notBefore, notAfter, subject, keyPair.getPublic());
        
        // Sign the certificate
        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC")
                .build(keyPair.getPrivate());
        
        X509CertificateHolder certHolder = certBuilder.build(contentSigner);
        certificate = new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certHolder);
    }
    
    /**
     * Returns the encoded certificate
     */
    public String getEncodedCertificate() throws Exception {
        if (certificate == null) {
            throw new IllegalStateException("Certificate not generated yet");
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(certificate.getEncoded());
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
    
    /**
     * Returns the private key
     */
    public PrivateKey getPrivateKey() {
        if (keyPair == null) {
            throw new IllegalStateException("Key pair not generated yet");
        }
        return keyPair.getPrivate();
    }
    
    /**
     * Returns the public key
     */
    public PublicKey getPublicKey() {
        if (keyPair == null) {
            throw new IllegalStateException("Key pair not generated yet");
        }
        return keyPair.getPublic();
    }
    
    /**
     * Verifies a certificate received from a remote party
     */
    public boolean verifyCertificate(String encodedCertificate) {
        try {
            // Decode certificate
            byte[] certBytes = Base64.getDecoder().decode(encodedCertificate);
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            Certificate cert = certFactory.generateCertificate(new ByteArrayInputStream(certBytes));
            
            // Verify certificate (in a real-world app, you would verify against a trusted CA)
            // For this simulation, we just check that it's a valid certificate format
            cert.verify(cert.getPublicKey());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Extracts the public key from a certificate
     */
    public PublicKey extractPublicKey(String encodedCertificate) throws Exception {
        byte[] certBytes = Base64.getDecoder().decode(encodedCertificate);
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        Certificate cert = certFactory.generateCertificate(new ByteArrayInputStream(certBytes));
        return cert.getPublicKey();
    }
}
