package com.dpas.client;

import com.dpas.HelloWorld;
import com.dpas.HelloWorldServiceGrpc;
import com.dpas.crypto.Main;
import com.google.protobuf.ByteString;

import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

public class ClientAPI {
    public static ClientAPI instance = null;

    public static ClientAPI getInstance() {
        if(instance == null){
            instance = new ClientAPI();
        }
        return instance;
    }

    public void receive(HelloWorldServiceGrpc.HelloWorldServiceBlockingStub stub, String input) throws Exception {
        String[] command = input.split("\\|");
        System.out.println("Command: " + command[0]);
        switch (command[0]){
            case "register":
                register(stub, command);
                break;
            case "post":
                post(stub, command);
                break;
            case "postGeneral":
                postGeneral(stub, command);
                break;
            case "read":
                read(stub, command);
                break;
            case "readGeneral":
                readGeneral(stub, command);
                break;
            default:
                System.err.println("Invalid input.");
                break;

        }
    }

    private static KeyPair keys(String alias)
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

    private byte[] getSignature(byte[] hash, String userAlias) throws Exception {
        KeyPair keys = keys(userAlias);
        PrivateKey privateKey = keys.getPrivate();

        byte[] signature = Main.getSignatureFromHash(hash, privateKey);
        return signature;
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

    private void validate(ByteString sigByteString, String userAlias, String message, ByteString hashByteString) throws Exception {

        byte[] signature = sigByteString.toByteArray();
        byte[] hash = hashByteString.toByteArray();
        byte[] messageHash = Main.getHashFromObject(message);

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

    /*----------------------------------------------------------------------------------------------------------------*/
    /*------------------------------------------------COMMANDS--------------------------------------------------------*/
    /*----------------------------------------------------------------------------------------------------------------*/
    private void register(HelloWorldServiceGrpc.HelloWorldServiceBlockingStub stub, String[] command) {
        HelloWorld.RegisterRequest requestRegister = HelloWorld.RegisterRequest.newBuilder().setKey(command[1]).build();
        HelloWorld.RegisterResponse responseRegister = stub.register(requestRegister);
        System.out.println("REGISTER: " + responseRegister);
    }

    /*--------------------------------------------------POSTS---------------------------------------------------------*/
    private HelloWorld.Announcement buildAnnouncement(HelloWorldServiceGrpc.HelloWorldServiceBlockingStub stub, String[] command) {
        List<HelloWorld.Announcement> referral = new ArrayList<HelloWorld.Announcement>();
        if(command.length > 2){
            int i = 3;
            while(i < command.length){
                referral.add(HelloWorld.Announcement.newBuilder().setKey(command[i]).setMessage(command[i+1]).build());
                i+=2;
            }
        }

        HelloWorld.GetTokenRequest requestGetToken = HelloWorld.GetTokenRequest.newBuilder().setKey(command[1]).build();
        HelloWorld.GetTokenResponse responseGetToken = stub.getToken(requestGetToken);
        System.out.println("GET TOKEN: " + responseGetToken);

        HelloWorld.Announcement.Builder post = HelloWorld.Announcement.newBuilder().setKey(command[1]).setMessage(command[2]).setToken(responseGetToken.getToken());
        for (HelloWorld.Announcement ref : referral) {
            post.addA(ref);
        }

        return post.build();
    }

    private void post(HelloWorldServiceGrpc.HelloWorldServiceBlockingStub stub, String[] command) throws Exception {
        HelloWorld.Announcement post = buildAnnouncement(stub, command);
        byte[] hash = Main.getHashFromObject(command);
        byte[] signature = getSignature(hash, command[1]);
        HelloWorld.PostRequest requestPost = HelloWorld.PostRequest.newBuilder().setPost(post).setSignature(ByteString.copyFrom(signature)).setHash(ByteString.copyFrom(hash)).build();
        HelloWorld.PostResponse responsePost = stub.post(requestPost);
        System.out.println("POST: " + responsePost);
    }

    private void postGeneral(HelloWorldServiceGrpc.HelloWorldServiceBlockingStub stub, String[] command) throws Exception {
        HelloWorld.Announcement post = buildAnnouncement(stub, command);
        byte[] hash = Main.getHashFromObject(command);
        byte[] signature = getSignature(hash, command[1]);
        HelloWorld.PostGeneralRequest requestGeneralPost = HelloWorld.PostGeneralRequest.newBuilder().setPost(post).setSignature(ByteString.copyFrom(signature)).setHash(ByteString.copyFrom(hash)).build();
        HelloWorld.PostGeneralResponse responseGeneralPost = stub.postGeneral(requestGeneralPost);
        System.out.println("POST: " + responseGeneralPost);
    }

    /*--------------------------------------------------READS---------------------------------------------------------*/
    private void read(HelloWorldServiceGrpc.HelloWorldServiceBlockingStub stub, String[] command) throws Exception {
        HelloWorld.ReadRequest requestRead = HelloWorld.ReadRequest.newBuilder().setNumber(Integer.parseInt(command[2])).setKey(command[1]).build();
        HelloWorld.ReadResponse responseRead = stub.read(requestRead);

        ByteString sigByteString = responseRead.getSignature();
        ByteString hashByteString = responseRead.getHash();
        String userAlias = responseRead.getResult(0).getKey();
        String message = responseRead.getResult(0).getMessage();

        validate(sigByteString, userAlias, message, hashByteString);

        System.out.println("READ: " + responseRead);
    }

    private void readGeneral(HelloWorldServiceGrpc.HelloWorldServiceBlockingStub stub, String[] command) throws Exception {
        HelloWorld.ReadGeneralRequest requestReadGeneral = HelloWorld.ReadGeneralRequest.newBuilder().setNumber(Integer.parseInt(command[1])).build();
        HelloWorld.ReadGeneralResponse responseReadGeneral = stub.readGeneral(requestReadGeneral);

        ByteString sigByteString = responseReadGeneral.getSignature();
        ByteString hashByteString = responseReadGeneral.getHash();
        String userAlias = responseReadGeneral.getResult(1).getKey();
        String message = responseReadGeneral.getResult(0).getMessage();

        validate(sigByteString, userAlias, message, hashByteString);

        System.out.println("READ: " + responseReadGeneral);
    }
}
