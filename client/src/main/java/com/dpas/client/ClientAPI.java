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

    
    /*----------------------------------------------------------------------------------------------------------------*/
    /*------------------------------------------------COMMANDS--------------------------------------------------------*/
    /*----------------------------------------------------------------------------------------------------------------*/
    public void register(HelloWorldServiceBlockingStub stub, String[] command) {
        //TODO:Have client send signature so server can verify identity
        HelloWorld.RegisterRequest requestRegister = HelloWorld.RegisterRequest.newBuilder().setKey(command[1]).build();
        HelloWorld.RegisterResponse responseRegister = stub.register(requestRegister);
        System.out.println("REGISTER: " + responseRegister);
    }

    /*--------------------------------------------------POSTS---------------------------------------------------------*/
    public Announcement buildAnnouncement(String[] command) {
        List<Announcement> referral = new ArrayList<Announcement>();
        if(command.length > 2){
            int i = 3;
            while(i < command.length-1){
                referral.add(Announcement.newBuilder().setKey(command[i]).setMessage(command[i+1]).build());
                i+=2;
            }
        }
        Announcement.Builder post = Announcement.newBuilder().setKey(command[1]).setMessage(command[2]);
        post.addAllA(referral);

        return post.build();
    }

    public void post(HelloWorldServiceBlockingStub stub, String[] command) throws Exception {
        Announcement post = buildAnnouncement(command);

        GetTokenRequest requestGetToken = GetTokenRequest.newBuilder().setKey(command[1]).build();
        GetTokenResponse responseGetToken = stub.getToken(requestGetToken);
        System.out.println("GET TOKEN: " + responseGetToken);

        byte[] hash = Main.getHashFromObject(command[2]);
        byte[] signature = Main.getSignature(hash, command[1]);

        PostRequest requestPost = PostRequest.newBuilder().setPost(post).setSignature(ByteString.copyFrom(signature))
                .setHash(ByteString.copyFrom(hash)).setToken(responseGetToken.getToken()).build();
        PostResponse responsePost = stub.post(requestPost);
        System.out.println("POST: " + responsePost);
    }

    public void postGeneral(HelloWorldServiceBlockingStub stub, String[] command) throws Exception {
        Announcement post = buildAnnouncement(command);

        GetTokenRequest requestGetToken = GetTokenRequest.newBuilder().setKey(command[1]).build();
        GetTokenResponse responseGetToken = stub.getToken(requestGetToken);
        System.out.println("GET TOKEN: " + responseGetToken);

        byte[] hash = Main.getHashFromObject(command[2]);
        byte[] signature = Main.getSignature(hash, command[1]);

        PostGeneralRequest requestGeneralPost = PostGeneralRequest.newBuilder()
                .setPost(post).setSignature(ByteString.copyFrom(signature))
                .setHash(ByteString.copyFrom(hash)).setToken(responseGetToken.getToken()).build();

        PostGeneralResponse responseGeneralPost = stub.postGeneral(requestGeneralPost);
        System.out.println("POST: " + responseGeneralPost);
    }

    /*--------------------------------------------------READS---------------------------------------------------------*/
    public void read(HelloWorldServiceBlockingStub stub, String[] command) throws Exception {
        ReadRequest requestRead = ReadRequest.newBuilder().setNumber(Integer.parseInt(command[2])).setKey(command[1]).build();
        ReadResponse responseRead = stub.read(requestRead);

        //TODO:Verify server integrity and authenticity when its implemented in server side
//        ByteString sigByteString = responseRead.getSignature();
//        ByteString hashByteString = responseRead.getHash();
//
//        String userAlias = responseRead.getResult(0).getKey();
//        String message = responseRead.getResult(0).getMessage();
//
//        byte[] signature = sigByteString.toByteArray();
//        byte[] hash = hashByteString.toByteArray();
//        byte[] messageHash = Main.getHashFromObject(message);
//
//        Main.validate(signature, userAlias, messageHash, hash);

        System.out.println("READ: " + responseRead);
    }

    public void readGeneral(HelloWorldServiceBlockingStub stub, String[] command) throws Exception {
        ReadGeneralRequest requestReadGeneral = ReadGeneralRequest.newBuilder().setNumber(Integer.parseInt(command[1])).build();
        ReadGeneralResponse responseReadGeneral = stub.readGeneral(requestReadGeneral);

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

        System.out.println("READ: " + responseReadGeneral);
    }
}
