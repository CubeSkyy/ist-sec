package com.dpas.client;

import com.dpas.Dpas.*;
import com.dpas.DpasServiceGrpc.DpasServiceBlockingStub;
import com.dpas.crypto.Main;
import com.google.protobuf.ByteString;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

public class ClientAPI {
    public static ClientAPI instance = null;

    public static ClientAPI getInstance() {
        if (instance == null) {
            instance = new ClientAPI();
        }
        return instance;
    }

    public void receive(DpasServiceBlockingStub stub, String input) throws Exception {
        try {
            String[] command = getCommand(input);
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
        } catch (io.grpc.StatusRuntimeException e) {
            System.err.println(e.getMessage());
        }
    }

    public String[] getCommand(String command) {
        return command.split("\\|");
    }

    public GetTokenResponse getClientToken(DpasServiceBlockingStub stub, String userAlias) throws Exception {
        byte[] keyHash = Main.getHashFromObject(userAlias);
        byte[] keySig = Main.getSignature(keyHash, userAlias);

        GetTokenRequest requestGetToken = GetTokenRequest.newBuilder().setKey(userAlias)
                .setSignature(ByteString.copyFrom(keySig)).build();
        GetTokenResponse responseGetToken = stub.getToken(requestGetToken);
        System.out.println("GET TOKEN: " + responseGetToken);
        return responseGetToken;
    }

    public boolean validateToken(GetTokenResponse responseGetToken) throws Exception {
        ByteString serverSigByteString = responseGetToken.getSignature();
        String token = responseGetToken.getToken();

        byte[] serverSig = serverSigByteString.toByteArray();
        byte[] tokenHash = Main.getHashFromObject(token);

        boolean valid = Main.validate(serverSig, "server1", tokenHash);
        return valid;
    }

    public boolean validateServerResponse(ByteString signature, byte[] hash) throws Exception {
        byte[] responseSignature = signature.toByteArray();
        boolean validResponse = Main.validate(responseSignature, "server1", hash);
        return validResponse;
    }

    /*----------------------------------------------------------------------------------------------------------------*/
    /*------------------------------------------------COMMANDS--------------------------------------------------------*/
    /*----------------------------------------------------------------------------------------------------------------*/
    public RegisterResponse register(DpasServiceBlockingStub stub, String[] command) throws Exception {
        String userAlias = command[1];

        byte[] hash = Main.getHashFromObject(userAlias);
        byte[] signature = Main.getSignature(hash, userAlias);

        RegisterRequest requestRegister = RegisterRequest.newBuilder().setKey(command[1])
                .setSignature(ByteString.copyFrom(signature)).build();

        RegisterResponse responseRegister = stub.register(requestRegister);

        /*---------------------------------SERVER VALIDATION--------------------------------*/
        ByteString sigServerByteString = responseRegister.getSignature();
        String key = responseRegister.getResult();

        byte[] resultHash = Main.getHashFromObject(key);

        boolean validResponse = validateServerResponse(sigServerByteString, resultHash);
        if (!validResponse) {
            System.err.println("Invalid signature and/or hash. Register response corrupted.");
            return null;
        }

        System.out.println("REGISTER: " + responseRegister);
        return responseRegister;
    }

    /*--------------------------------------------------POSTS---------------------------------------------------------*/
    public Announcement buildAnnouncement(String[] command) {
        List<Integer> referral = new ArrayList<Integer>();
        if (command.length > 2) {
            int i = 3;
            while (i < command.length) {
                referral.add(Integer.parseInt(command[i]));
                i += 1;
            }
        }
        Announcement.Builder post = Announcement.newBuilder().setKey(command[1]).setMessage(command[2]);
        post.addAllRef(referral);

        return post.build();
    }

    public boolean post(DpasServiceBlockingStub stub, String[] command) throws Exception {

        String userAlias = command[1];
        /*-------------------------------GET TOKEN VALIDATION-------------------------------*/
        GetTokenResponse responseGetToken = getClientToken(stub, userAlias);

        boolean tokenVerify = validateToken(responseGetToken);

        if (!tokenVerify) {
            System.err.println("Invalid signature and/or hash. GetToken request corrupted.");
            return false;
        }
        /*----------------------------------------------------------------------------------*/
        String token = responseGetToken.getToken();
        Announcement post = buildAnnouncement(command);

        byte[] postHash = Main.getHashFromObject(post);
        byte[] tokenHash = Main.getHashFromObject(token);
        byte[] hash = ArrayUtils.addAll(postHash, tokenHash);
        byte[] signature = Main.getSignature(hash, userAlias);

        PostRequest requestPost = PostRequest.newBuilder().setPost(post).setSignature(ByteString.copyFrom(signature)).setToken(token).build();
        PostResponse responsePost = stub.post(requestPost);

        /*---------------------------------SERVER VALIDATION--------------------------------*/
        ByteString sigServerByteString = responsePost.getSignature();
        String key = responsePost.getResult();

        byte[] resultHash = Main.getHashFromObject(key);

        boolean validResponse = validateServerResponse(sigServerByteString, resultHash);
        if (!validResponse) {
            System.err.println("Invalid signature and/or hash. Post response corrupted.");
        } else {
            System.out.println("POST: " + responsePost);
        }


        return validResponse;
    }


    public boolean postGeneral(DpasServiceBlockingStub stub, String[] command) throws Exception {
        String userAlias = command[1];
        /*-------------------------------GET TOKEN VALIDATION-------------------------------*/
        GetTokenResponse responseGetToken = getClientToken(stub, userAlias);

        boolean tokenVerify = validateToken(responseGetToken);

        if (!tokenVerify) {
            System.err.println("Invalid signature and/or hash. GetToken request corrupted.");
            return false;
        }
        /*----------------------------------------------------------------------------------*/
        String token = responseGetToken.getToken();
        Announcement post = buildAnnouncement(command);

        byte[] postHash = Main.getHashFromObject(post);
        byte[] tokenHash = Main.getHashFromObject(token);
        byte[] hash = ArrayUtils.addAll(postHash, tokenHash);
        byte[] signature = Main.getSignature(hash, userAlias);

        PostGeneralRequest requestGeneralPost = PostGeneralRequest.newBuilder()
                .setPost(post).setSignature(ByteString.copyFrom(signature)).setToken(token).build();

        PostGeneralResponse responseGeneralPost = stub.postGeneral(requestGeneralPost);
        /*---------------------------------SERVER VALIDATION--------------------------------*/
        ByteString sigServerByteString = responseGeneralPost.getSignature();
        String key = responseGeneralPost.getResult();

        byte[] resultHash = Main.getHashFromObject(key);

        boolean validResponse = validateServerResponse(sigServerByteString, resultHash);
        if (!validResponse) {
            System.err.println("Invalid signature and/or hash. Post General response corrupted.");
        } else {
            System.out.println("POST GENERAL: " + responseGeneralPost);
        }

        return validResponse;
    }

    /*--------------------------------------------------READS---------------------------------------------------------*/
    public ReadResponse read(DpasServiceBlockingStub stub, String[] command) throws Exception {
        String userAlias = command[1];
        String key = command[2];
        int number = Integer.parseInt(command[3]);
        /*-------------------------------GET TOKEN VALIDATION-------------------------------*/
        GetTokenResponse responseGetToken = getClientToken(stub, userAlias);

        boolean tokenVerify = validateToken(responseGetToken);

        if (!tokenVerify) {
            System.err.println("Invalid signature and/or hash. GetToken request corrupted.");
            return null;
        }
        /*----------------------------------------------------------------------------------*/
        String token = responseGetToken.getToken();
        byte[] keyHash = Main.getHashFromObject(key);
        byte[] numberHash = Main.getHashFromObject(number);
        byte[] userAliasHash = Main.getHashFromObject(userAlias);
        byte[] tokenHash = Main.getHashFromObject(token);

        byte[] hash = ArrayUtils.addAll(userAliasHash, keyHash); //userAlias + key + number + token
        hash = ArrayUtils.addAll(hash, numberHash);
        hash = ArrayUtils.addAll(hash, tokenHash);
        byte[] signature = Main.getSignature(hash, userAlias);

        ReadRequest requestRead = ReadRequest.newBuilder().setNumber(number).setKey(userAlias)
                .setKeyToRead(key).setSignature(ByteString.copyFrom(signature)).setToken(token).build();
        ReadResponse responseRead = stub.read(requestRead);

        /*---------------------------------SERVER VALIDATION--------------------------------*/
        ByteString sigServerByteString = responseRead.getSignature();
        ArrayList<Announcement> result = new ArrayList<Announcement>(responseRead.getResultList());
        byte[] resultHash = Main.getHashFromObject(result);

        boolean validResponse = validateServerResponse(sigServerByteString, resultHash);
        if (!validResponse) {
            System.err.println("Invalid signature and/or hash. Read Response corrupted.");
            return null;
        }

        System.out.println("READ: " + responseRead.getResultList());
        return responseRead;
    }


    public ReadGeneralResponse readGeneral(DpasServiceBlockingStub stub, String[] command) throws Exception {

        String userAlias = command[1];
        int number = Integer.parseInt(command[2]);
        /*-------------------------------GET TOKEN VALIDATION-------------------------------*/
        GetTokenResponse responseGetToken = getClientToken(stub, userAlias);

        boolean tokenVerify = validateToken(responseGetToken);

        if (!tokenVerify) {
            System.err.println("Invalid signature and/or hash. GetToken request corrupted.");
            return null;
        }
        /*----------------------------------------------------------------------------------*/
        String token = responseGetToken.getToken();
        byte[] numberHash = Main.getHashFromObject(number);
        byte[] userAliasHash = Main.getHashFromObject(userAlias);
        byte[] tokenHash = Main.getHashFromObject(token);

        byte[] hash = ArrayUtils.addAll(userAliasHash, numberHash); //userAlias + number + token
        hash = ArrayUtils.addAll(hash, tokenHash);
        byte[] signature = Main.getSignature(hash, userAlias);

        ReadGeneralRequest requestReadGeneral = ReadGeneralRequest.newBuilder().setNumber(number)
                .setKey(userAlias).setSignature(ByteString.copyFrom(signature)).setToken(token).build();
        ReadGeneralResponse responseReadGeneral = stub.readGeneral(requestReadGeneral);

        /*---------------------------------SERVER VALIDATION--------------------------------*/
        ByteString sigServerByteString = responseReadGeneral.getSignature();
        ArrayList<Announcement> result = new ArrayList<Announcement>(responseReadGeneral.getResultList());
        byte[] resultHash = Main.getHashFromObject(result);

        boolean validResponse = validateServerResponse(sigServerByteString, resultHash);
        if (!validResponse) {
            System.err.println("Invalid signature and/or hash. Read General response corrupted.");
            return null;
        }

        System.out.println("READ GENERAL: " + responseReadGeneral.getResultList());
        return responseReadGeneral;
    }
}
