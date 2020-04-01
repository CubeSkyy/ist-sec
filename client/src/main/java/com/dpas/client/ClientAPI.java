package com.dpas.client;

import com.dpas.HelloWorld;
import com.dpas.HelloWorldServiceGrpc;
import com.dpas.crypto.Main;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import com.dpas.HelloWorld.Announcement;
import com.dpas.HelloWorld.GetTokenRequest;
import com.dpas.HelloWorld.GetTokenResponse;
import com.dpas.HelloWorld.PostRequest;
import com.dpas.HelloWorld.PostResponse;
import com.dpas.HelloWorld.PostGeneralRequest;
import com.dpas.HelloWorld.PostGeneralResponse;
import com.dpas.HelloWorld.ReadRequest;
import com.dpas.HelloWorld.ReadResponse;
import com.dpas.HelloWorld.ReadGeneralRequest;
import com.dpas.HelloWorld.ReadGeneralResponse;
import org.apache.commons.lang3.ArrayUtils;
import com.dpas.HelloWorldServiceGrpc.HelloWorldServiceBlockingStub;

public class ClientAPI {
    public static ClientAPI instance = null;

    public static ClientAPI getInstance() {
        if(instance == null){
            instance = new ClientAPI();
        }
        return instance;
    }

    public void receive(HelloWorldServiceBlockingStub stub, String input) throws Exception {
        try {
            String[] command = input.split("\\|");
            System.out.println("Command: " + command[0]);
            switch (command[0]) {
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
        }catch (io.grpc.StatusRuntimeException e){
            System.err.println(e.getMessage());
        }
    }

    
    /*----------------------------------------------------------------------------------------------------------------*/
    /*------------------------------------------------COMMANDS--------------------------------------------------------*/
    /*----------------------------------------------------------------------------------------------------------------*/
    public void register(HelloWorldServiceBlockingStub stub, String[] command) throws Exception {
        String userAlias = command[1];
        /*-------------------------------GET TOKEN VALIDATION-------------------------------*/
        byte[] keyHash = Main.getHashFromObject(userAlias);
        byte[] keySig = Main.getSignature(keyHash, userAlias);

        GetTokenRequest requestGetToken = GetTokenRequest.newBuilder().setKey(userAlias).setSignature(ByteString.copyFrom(keySig))
                .setHash(ByteString.copyFrom(keyHash)).build();
        GetTokenResponse responseGetToken = stub.getToken(requestGetToken);
        System.out.println("GET TOKEN: " + responseGetToken);

        ByteString serverSigByteString = responseGetToken.getSignature();
        ByteString serverHashByteString = responseGetToken.getHash();
        String token = responseGetToken.getToken();

        byte[] serverSig = serverSigByteString.toByteArray();
        byte[] serverHash = serverHashByteString.toByteArray();
        byte[] tokenHash = Main.getHashFromObject(token);

        boolean valid = Main.validate(serverSig, "server1", tokenHash, serverHash); //TODO change to serverAlias when we have multiple servers
        if(!valid){
            System.err.println("Invalid signature and/or hash. GetToken request corrupted.");
        }
        /*----------------------------------------------------------------------------------*/

        byte[] userAliasHash = Main.getHashFromObject(command[1]);

        byte[] hash = ArrayUtils.addAll(userAliasHash, tokenHash);
        byte[] signature = Main.getSignature(hash, command[1]);

        HelloWorld.RegisterRequest requestRegister = HelloWorld.RegisterRequest.newBuilder().setKey(command[1]).setSignature(ByteString.copyFrom(signature))
                .setHash(ByteString.copyFrom(hash)).setToken(token).build();
        HelloWorld.RegisterResponse responseRegister = stub.register(requestRegister);
        System.out.println("REGISTER: " + responseRegister);
    }

    /*--------------------------------------------------POSTS---------------------------------------------------------*/
    public Announcement buildAnnouncement(String[] command) {
        List<Integer> referral = new ArrayList<Integer>();
        if(command.length > 2){
            int i = 3;
            while(i < command.length){
                referral.add(Integer.parseInt(command[i]));
                i+=1;
            }
        }
        Announcement.Builder post = Announcement.newBuilder().setKey(command[1]).setMessage(command[2]);
        post.addAllRef(referral);

        return post.build();
    }

    public void post(HelloWorldServiceBlockingStub stub, String[] command) throws Exception {
        Announcement post = buildAnnouncement(command);

        String userAlias = command[1];
        /*-------------------------------GET TOKEN VALIDATION-------------------------------*/
        byte[] keyHash = Main.getHashFromObject(userAlias);
        byte[] keySig = Main.getSignature(keyHash, userAlias);

        GetTokenRequest requestGetToken = GetTokenRequest.newBuilder().setKey(userAlias).setSignature(ByteString.copyFrom(keySig))
                .setHash(ByteString.copyFrom(keyHash)).build();
        GetTokenResponse responseGetToken = stub.getToken(requestGetToken);
        System.out.println("GET TOKEN: " + responseGetToken);

        ByteString serverSigByteString = responseGetToken.getSignature();
        ByteString serverHashByteString = responseGetToken.getHash();
        String token = responseGetToken.getToken();

        byte[] serverSig = serverSigByteString.toByteArray();
        byte[] serverHash = serverHashByteString.toByteArray();
        byte[] tokenHash = Main.getHashFromObject(token);

        boolean valid = Main.validate(serverSig, "server1", tokenHash, serverHash); //TODO change to serverAlias when we have multiple servers
        if(!valid){
            System.err.println("Invalid signature and/or hash. GetToken request corrupted.");
        }
        /*----------------------------------------------------------------------------------*/

        byte[] postHash = Main.getHashFromObject(post);

        byte[] hash = ArrayUtils.addAll(postHash, tokenHash);
        byte[] signature = Main.getSignature(hash, command[1]);

        PostRequest requestPost = PostRequest.newBuilder().setPost(post).setSignature(ByteString.copyFrom(signature))
                .setHash(ByteString.copyFrom(hash)).setToken(responseGetToken.getToken()).build();
        PostResponse responsePost = stub.post(requestPost);
        System.out.println("POST: " + responsePost);
    }

    public void postGeneral(HelloWorldServiceBlockingStub stub, String[] command) throws Exception {
        Announcement post = buildAnnouncement(command);

        String userAlias = command[1];
        /*-------------------------------GET TOKEN VALIDATION-------------------------------*/
        byte[] keyHash = Main.getHashFromObject(userAlias);
        byte[] keySig = Main.getSignature(keyHash, userAlias);

        GetTokenRequest requestGetToken = GetTokenRequest.newBuilder().setKey(userAlias).setSignature(ByteString.copyFrom(keySig))
                .setHash(ByteString.copyFrom(keyHash)).build();
        GetTokenResponse responseGetToken = stub.getToken(requestGetToken);
        System.out.println("GET TOKEN: " + responseGetToken);

        ByteString serverSigByteString = responseGetToken.getSignature();
        ByteString serverHashByteString = responseGetToken.getHash();
        String token = responseGetToken.getToken();

        byte[] serverSig = serverSigByteString.toByteArray();
        byte[] serverHash = serverHashByteString.toByteArray();
        byte[] tokenHash = Main.getHashFromObject(token);

        boolean valid = Main.validate(serverSig, "server1", tokenHash, serverHash); //TODO change to serverAlias when we have multiple servers
        if(!valid){
            System.err.println("Invalid signature and/or hash. GetToken request corrupted.");
        }
        /*----------------------------------------------------------------------------------*/

        byte[] postHash = Main.getHashFromObject(post);
        byte[] hash = ArrayUtils.addAll(postHash, tokenHash);
        byte[] signature = Main.getSignature(hash, command[1]);

        PostGeneralRequest requestGeneralPost = PostGeneralRequest.newBuilder()
                .setPost(post).setSignature(ByteString.copyFrom(signature))
                .setHash(ByteString.copyFrom(hash)).setToken(token).build();

        PostGeneralResponse responseGeneralPost = stub.postGeneral(requestGeneralPost);
        System.out.println("POST GENERAL: " + responseGeneralPost);
    }

    /*--------------------------------------------------READS---------------------------------------------------------*/
    public void read(HelloWorldServiceBlockingStub stub, String[] command) throws Exception {
        String userAlias = command[1];
        String key = command[2];
        int number = Integer.parseInt(command[3]);
        /*-------------------------------GET TOKEN VALIDATION-------------------------------*/
        byte[] userAliasHash = Main.getHashFromObject(userAlias);
        byte[] keySig = Main.getSignature(userAliasHash, userAlias);

        GetTokenRequest requestGetToken = GetTokenRequest.newBuilder().setKey(userAlias).setSignature(ByteString.copyFrom(keySig))
                .setHash(ByteString.copyFrom(userAliasHash)).build();
        GetTokenResponse responseGetToken = stub.getToken(requestGetToken);
        System.out.println("GET TOKEN: " + responseGetToken);

        ByteString serverSigByteString = responseGetToken.getSignature();
        ByteString serverHashByteString = responseGetToken.getHash();
        String token = responseGetToken.getToken();

        byte[] serverSig = serverSigByteString.toByteArray();
        byte[] serverHash = serverHashByteString.toByteArray();
        byte[] tokenHash = Main.getHashFromObject(token);

        boolean valid = Main.validate(serverSig, "server1", tokenHash, serverHash); //TODO change to serverAlias when we have multiple servers
        if(!valid){
            System.err.println("Invalid signature and/or hash. GetToken request corrupted.");
        }
        /*----------------------------------------------------------------------------------*/

        byte[] keyHash = Main.getHashFromObject(key);
        byte[] numberHash = Main.getHashFromObject(number);
        byte[] hash = ArrayUtils.addAll(userAliasHash, keyHash); //userAlias + key + number + token
        hash = ArrayUtils.addAll(hash, numberHash);
        hash = ArrayUtils.addAll(hash, tokenHash);
        byte[] signature = Main.getSignature(hash, userAlias);

        ReadRequest requestRead = ReadRequest.newBuilder().setNumber(number)
                .setKey(userAlias).setKeyToRead(key).setSignature(ByteString.copyFrom(signature))
                .setHash(ByteString.copyFrom(hash)).setToken(token).build();
        ReadResponse responseRead = stub.read(requestRead);

        /*---------------------------------SERVER VALIDATION--------------------------------*/
        ByteString sigServerByteString = responseRead.getSignature();
        ByteString hashServerByteString = responseRead.getHash();

        ArrayList<Announcement> result = new ArrayList<Announcement>(responseRead.getResultList());

        byte[] responseSignature = sigServerByteString.toByteArray();
        byte[] responseHash = hashServerByteString.toByteArray();
        byte[] resultHash = Main.getHashFromObject(result);

        boolean validResponse = Main.validate(responseSignature, "server1", resultHash, responseHash); //TODO change to serverAlias when we have multiple servers
        if(!validResponse){
            System.err.println("Invalid signature and/or hash. Read request corrupted.");
        }

        System.out.println("READ: " + responseRead.getResultList());
    }

    public void readGeneral(HelloWorldServiceBlockingStub stub, String[] command) throws Exception {


        //TODO:Verify server integrity and authenticity when its implemented in server side
//        ByteString sigByteString = responseReadGeneral.getSignature();
//        ByteString hashByteString = responseReadGeneral.getHash();
//
//
//        ArrayList<Announcement> message = new ArrayList<Announcement>();
//        for(int i = 0; i < responseReadGeneral.getResultCount(); i++){
//            message.add(responseReadGeneral.getResult(i));
//        }
//
//        byte[] signature = sigByteString.toByteArray();
//        byte[] hash = hashByteString.toByteArray();
//
//        byte[] messageHash = Main.getHashFromObject(message);
//        Main.validate(signature, "server1", messageHash, hash);
        String userAlias = command[1];
        int number = Integer.parseInt(command[2]);
        /*-------------------------------GET TOKEN VALIDATION-------------------------------*/
        byte[] userAliasHash = Main.getHashFromObject(userAlias);
        byte[] keySig = Main.getSignature(userAliasHash, userAlias);

        GetTokenRequest requestGetToken = GetTokenRequest.newBuilder().setKey(userAlias).setSignature(ByteString.copyFrom(keySig))
                .setHash(ByteString.copyFrom(userAliasHash)).build();
        GetTokenResponse responseGetToken = stub.getToken(requestGetToken);
        System.out.println("GET TOKEN: " + responseGetToken);

        ByteString serverSigByteString = responseGetToken.getSignature();
        ByteString serverHashByteString = responseGetToken.getHash();
        String token = responseGetToken.getToken();

        byte[] serverSig = serverSigByteString.toByteArray();
        byte[] serverHash = serverHashByteString.toByteArray();
        byte[] tokenHash = Main.getHashFromObject(token);

        boolean valid = Main.validate(serverSig, "server1", tokenHash, serverHash); //TODO change to serverAlias when we have multiple servers
        if(!valid){
            System.err.println("Invalid signature and/or hash. GetToken request corrupted.");
        }
        /*----------------------------------------------------------------------------------*/

        byte[] numberHash = Main.getHashFromObject(number);
        byte[] hash = ArrayUtils.addAll(userAliasHash, numberHash); //userAlias + number + token
        hash = ArrayUtils.addAll(hash, tokenHash);

        byte[] signature = Main.getSignature(hash, userAlias);

        ReadGeneralRequest requestReadGeneral = ReadGeneralRequest.newBuilder().setNumber(number)
                .setKey(userAlias).setSignature(ByteString.copyFrom(signature))
                .setHash(ByteString.copyFrom(hash)).setToken(token).build();
        ReadGeneralResponse responseReadGeneral = stub.readGeneral(requestReadGeneral);

        /*---------------------------------SERVER VALIDATION--------------------------------*/
        ByteString sigServerByteString = responseReadGeneral.getSignature();
        ByteString hashServerByteString = responseReadGeneral.getHash();

        ArrayList<Announcement> result = new ArrayList<Announcement>(responseReadGeneral.getResultList());

        byte[] responseSignature = sigServerByteString.toByteArray();
        byte[] responseHash = hashServerByteString.toByteArray();
        byte[] resultHash = Main.getHashFromObject(result);

        boolean validResponse = Main.validate(responseSignature, "server1", resultHash, responseHash); //TODO change to serverAlias when we have multiple servers
        if(!validResponse){
            System.err.println("Invalid signature and/or hash. Read request corrupted.");
        }

        System.out.println("READ GENERAL: " + responseReadGeneral.getResultList());
    }
}
