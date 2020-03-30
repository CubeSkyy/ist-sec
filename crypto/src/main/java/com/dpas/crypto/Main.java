package com.dpas.crypto;

import java.security.Key;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import static javax.xml.bind.DatatypeConverter.printHexBinary;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;


public class Main {

	/*final static String ASYM_CIPHER = "RSA/ECB/PKCS1Padding";

	public static Key readKey(String resourcePath) throws Exception {
		// TODO
		// altera o return type para o que achares melhor
		return null;
	}*/

	public static byte[] getHashFromObject (Object obj) throws Exception {
		// Make a hash of the object that we want to sign
		final String DIGEST_ALGO = "SHA-512";
		MessageDigest messageDigest = MessageDigest.getInstance(DIGEST_ALGO);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(bos);
		out.writeObject(obj);
		out.flush();
		byte[] bObj = bos.toByteArray();

		messageDigest.update(bObj);
		byte[] digest = messageDigest.digest();
		return digest;


	}

	/*--------------------------------------------------KEYS----------------------------------------------------------*/
    private static KeyPair getKeys(String alias)
            throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        //alias atuais => "leaf", "user1", "user2"

        FileInputStream fis = new FileInputStream("keystore.jks");
        KeyStore keyStore = KeyStore.getInstance("JCEKS");
        char[] pwd = "password".toCharArray();
        keyStore.load(fis, pwd);

        Key key = keyStore.getKey(alias, pwd);
        if(key instanceof PrivateKey){
            Certificate cert = keyStore.getCertificate(alias);
            PublicKey publicKey = cert.getPublicKey();
            KeyPair keyPair = new KeyPair(publicKey, (PrivateKey) key);
            return keyPair;
        }
        return null;
    }

    private static PrivateKey getPrivateKey(String userAlias)
            throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        KeyPair keys = getKeys(userAlias);
        return keys.getPrivate();

    }

    public static PublicKey getPublicKey(String userAlias)
            throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
        FileInputStream fis = new FileInputStream("keystore.jks");
        KeyStore keyStore = KeyStore.getInstance("JCEKS");
        char[] pwd = "password".toCharArray();
        keyStore.load(fis, pwd);
        Certificate cert = keyStore.getCertificate(userAlias);
        return cert.getPublicKey();
    }

    /*------------------------------------------------SIGNATURE-------------------------------------------------------*/
	public static byte[] getSignatureFromHash (byte[] hash, PrivateKey privateKey) throws Exception {

		/*Cipher cipher = Cipher.getInstance(ASYM_CIPHER);

		cipher.init(Cipher.ENCRYPT_MODE, key);
		byte[] cipherBytes = cipher.doFinal(hash);
		return cipherBytes;*/

		Signature signature = Signature.getInstance("SHA256withRSA");
		signature.initSign(privateKey);
		signature.update(hash);
		byte[] sign = signature.sign();
		return sign;
	}

	/*public static byte[] getHashFromSignature (byte[] signature, PublicKey publicKey) throws Exception {

		Cipher cipher = Cipher.getInstance(ASYM_CIPHER);

		cipher.init(Cipher.DECRYPT_MODE, publicKey);
		byte[] decipherBytes = cipher.doFinal(signature);
		return decipherBytes;


	}*/

    public static byte[] getSignature(byte[] hash, String userAlias) throws Exception {
        PrivateKey privateKey = getPrivateKey(userAlias);
        byte[] signature = getSignatureFromHash(hash, privateKey);
        return signature;
    }

    /*--------------------------------------------------VALIDATE------------------------------------------------------*/
    public static void validate(byte[] signature, String userAlias, String message, byte[] hash) throws Exception {

        byte[] messageHash = getHashFromObject(message);

        PublicKey publicKey = getPublicKey(userAlias);

        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(publicKey);
        sig.update(messageHash);
        boolean verify = sig.verify(signature);

        System.out.println("Signature is valid: " + verify);
        if(!verify){
            throw new Exception("Invalid signature! Board compromised!");
        }

        if(!messageHash.equals(hash)){
            throw new Exception("Hash does not correspond! Board compromised!");
        }
    }

	public static void main(String[] args) {

		System.out.println("Crypto is up");

	}

}
