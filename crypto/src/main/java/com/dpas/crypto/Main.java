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
import java.security.interfaces.RSAPublicKey;


public class Main {

	final static String ASYM_CIPHER = "RSA/ECB/PKCS1Padding";

	public static Key readKey(String resourcePath) throws Exception {
		// TODO
		// altera o return type para o que achares melhor
		return null;
	}

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
		//System.out.println("Digest value:");
		//System.out.println(printHexBinary(digest));
		return digest;


	}

	public static byte[] getSignatureFromHash (byte[] hash, PrivateKey key) throws Exception {

		Cipher cipher = Cipher.getInstance(ASYM_CIPHER);

		cipher.init(Cipher.ENCRYPT_MODE, key);
		byte[] cipherBytes = cipher.doFinal(hash);
		return cipherBytes;


	}

	public static byte[] getHashFromSignature (byte[] signature, RSAPublicKey key) throws Exception {

		Cipher cipher = Cipher.getInstance(ASYM_CIPHER);

		cipher.init(Cipher.DECRYPT_MODE, key);
		byte[] decipherBytes = cipher.doFinal(signature);
		return decipherBytes;


	}

	public static void main(String[] args) {

		System.out.println("Crypto is up");

	}

}
