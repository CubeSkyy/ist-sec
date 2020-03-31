package com.dpas.client;

import com.dpas.HelloWorld;
import com.dpas.HelloWorldServiceGrpc;
import com.dpas.crypto.Main;
import com.google.protobuf.ByteString;
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
            while(i < command.length-1){
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

        byte[] hash = Main.getHashFromObject(command[2]);
        byte[] signature = Main.getSignature(hash, command[1]);

        HelloWorld.PostRequest requestPost = HelloWorld.PostRequest.newBuilder().setPost(post).setSignature(ByteString.copyFrom(signature)).setHash(ByteString.copyFrom(hash)).build();
        HelloWorld.PostResponse responsePost = stub.post(requestPost);
        System.out.println("POST: " + responsePost);
    }

    private void postGeneral(HelloWorldServiceGrpc.HelloWorldServiceBlockingStub stub, String[] command) throws Exception {
        HelloWorld.Announcement post = buildAnnouncement(stub, command);

        byte[] hash = Main.getHashFromObject(command[2]);
        byte[] signature = Main.getSignature(hash, command[1]);

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

        byte[] signature = sigByteString.toByteArray();
        byte[] hash = hashByteString.toByteArray();
        byte[] messageHash = Main.getHashFromObject(message);

        Main.validate(signature, userAlias, messageHash, hash);

        System.out.println("READ: " + responseRead);
    }

    private void readGeneral(HelloWorldServiceGrpc.HelloWorldServiceBlockingStub stub, String[] command) throws Exception {
        HelloWorld.ReadGeneralRequest requestReadGeneral = HelloWorld.ReadGeneralRequest.newBuilder().setNumber(Integer.parseInt(command[1])).build();
        HelloWorld.ReadGeneralResponse responseReadGeneral = stub.readGeneral(requestReadGeneral);

        ByteString sigByteString = responseReadGeneral.getSignature();
        ByteString hashByteString = responseReadGeneral.getHash();


        ArrayList<HelloWorld.Announcement> message = new ArrayList<HelloWorld.Announcement>();
        for(int i = 0; i < responseReadGeneral.getResultCount(); i++){
            message.add(responseReadGeneral.getResult(i));
        }

        byte[] signature = sigByteString.toByteArray();
        byte[] hash = hashByteString.toByteArray();

        byte[] messageHash = Main.getHashFromObject(message);
        Main.validate(signature, "server1", messageHash, hash);

        System.out.println("READ: " + responseReadGeneral);
    }
}
