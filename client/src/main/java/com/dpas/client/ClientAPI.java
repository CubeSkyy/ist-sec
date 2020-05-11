package com.dpas.client;

import com.dpas.Dpas.*;
import com.dpas.DpasServiceGrpc.DpasServiceBlockingStub;
import com.dpas.crypto.Main;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.dpas.client.DpasClient.*;

interface API {
    GeneratedMessageV3 grpcOperation(DpasServiceBlockingStub stub, String[] command, ArrayList<BroadcastResponse> bcb) throws Exception;
}

public class ClientAPI {
    int wts = 0;
    int majority;

    public static ClientAPI instance = null;

    public static ClientAPI getInstance() {
        if (instance == null) {
            instance = new ClientAPI();
        }
        return instance;
    }

    //TODO: Add rid to normal signature
    //TODO: Make RID checks
    //TODO: Persist server timestamp?
    //TODO: Change majority to depend on faulty servers

    public String[] getCommand(String command) {
        return command.split("\\|");
    }

    private void printRead(List<Announcement> _list) {
        ArrayList<Announcement> list = new ArrayList<>(_list);
        if (list.isEmpty()) System.out.println("Empty.");
        else {
            for (Announcement a : list) {
                System.out.println(" {");
                System.out.println("     Key: " + a.getKey());
                System.out.println("     Message: " + a.getMessage());
                System.out.println("     PostId: " + a.getPostId());
                System.out.println("     References: " + a.getRefList());
                System.out.println(" }");
            }
        }
    }

    public void receive(ArrayList<DpasServiceBlockingStub> stubs, String input) throws Exception {
        try {
            String[] command = getCommand(input);
            String[] commands;
            List<GeneratedMessageV3> responses;
            System.out.println("\nCommand: " + command[0] + "\n");
            switch (command[0]) {
                case "register":
                    if (command.length != 2) {
                        System.err.println("Usage: register|<userAlias>");
                        break;
                    }
                    responses = sendAsync(stubs, command, this::register, null);
                    if (responses != null) {
                        RegisterResponse response = (RegisterResponse) responses.get(0);
                        System.out.println("REGISTER COMPLETE: " + response.getResult());
                    }
                    break;
                case "post":
                    if (command.length < 3) {
                        System.err.println("Usage: post|<userAlias>|<Message>|<Reference List>\nReference List can be empty.");
                        break;
                    }

                    wts++;
                    Announcement requestPost = buildAnnouncement(command);
                    ArrayList<BroadcastResponse> bcb = sendBCB(stubs, requestPost);
                    if (bcb == null) {
                        System.err.println("BCB failed. Command not executed.");
                        break;
                    }

                    responses = sendAsync(stubs, command, this::post, bcb);
                    if (responses != null) {
                        PostResponse response = (PostResponse) responses.get(0);
                        System.out.println("POST DONE: " + response.getResult());
                    }

                    break;
                case "postGeneral":
                    if (command.length < 3) {
                        System.err.println("Usage: postGeneral|<userAlias>|<Message>|<Reference List>\nReference List can be empty.");
                        break;
                    }

                    responses = sendAsyncNN(stubs, command, this::postGeneral);
                    if (responses != null) {
                        PostGeneralResponse response = (PostGeneralResponse) responses.get(0);
                        System.out.println("POST GENERAL DONE: " + response.getResult());
                    }

                    break;
                case "read":
                    if (command.length != 4) {
                        System.err.println("Usage: read|<userAlias>|<userToRead>|<NumberOfPosts>.");
                        break;
                    }
                    responses = readAsync(stubs, command, this::read, false, null);
                    ReadResponse message = null;
                    ReadResponse result = null;
                    int maxTs = -2;
                    for (GeneratedMessageV3 m : responses) {
                        message = (ReadResponse) m;
                        if (maxTs < message.getTs()) {
                            maxTs = message.getTs();
                            result = message;
                        }
                    }
                    System.out.println("READ:");
                    printRead(result.getResultList());
                    break;
                case "readGeneral":
                    if (command.length != 3) {
                        System.err.println("Usage: readGeneral|<userAlias>|<NumberOfPosts>.");
                        break;
                    }
                    responses = readAsync(stubs, command, this::readGeneral, true, null);
                    ReadGeneralResponse resultGeneral = null;
                    int maxTsGeneral = -2;
                    for (GeneratedMessageV3 m : responses) {
                        ReadGeneralResponse messageGeneral = (ReadGeneralResponse) m;

                        if (maxTsGeneral < messageGeneral.getTs()) {
                            maxTsGeneral = messageGeneral.getTs();
                            resultGeneral = messageGeneral;
                        }
                    }
                    System.out.println("READ GENERAL:");
                    printRead(resultGeneral.getResultList());
                    break;
                case "reset":
                    wts = 0;
                    responses = sendAsync(stubs, command, this::reset, null);
                    if (responses != null) {
                        System.out.println("RESET DONE.");
                    }
                    break;
                case "demo1":
                    //DEMO1 = "register|user1\npost|user1|Test\nread|user1|user1|0";
                    receive(stubs, "reset");
                    commands = DEMO1.split("\n");
                    for (String com : commands) {
                        receive(stubs, com);
                    }
                    break;
                case "demo2":
                    //DEMO2 = "register|user1\nregister|user2\npostGeneral|user1|Test\npostGeneral|user2|Test2|1\nreadGeneral|user1|0";
                    receive(stubs, "reset");
                    commands = DEMO2.split("\n");
                    for (String com : commands) {
                        receive(stubs, com);
                    }
                    break;
                case "demo3":
                    //DEMO3 = "register|user1\npost|user1|Test\npost|user1|Test2\npost|user1|Test3\nread|user1|user1|2";
                    receive(stubs, "reset");
                    commands = DEMO3.split("\n");
                    for (String com : commands) {
                        receive(stubs, com);
                    }
                    break;
                default:
                    System.err.println("Invalid input.");
                    break;

            }
        } catch (io.grpc.StatusRuntimeException e) {
            System.err.println(e.getMessage());
        }
    }

    private ArrayList<BroadcastResponse> sendBCB(ArrayList<DpasServiceBlockingStub> stubs, Announcement message) {
        AtomicInteger counter = new AtomicInteger(0);
        ArrayList<BroadcastResponse> result = new ArrayList<>();
        stubs.stream().map(
                (DpasServiceBlockingStub stub) -> CompletableFuture.supplyAsync(
                        () -> {
                            try {
                                BroadcastResponse res = broadcast(stub, message);
                                byte[] msgHash = Main.getHashFromObject(message);
                                if (validateServerResponse(res.getSignature(), msgHash)) return res;
                                else {
                                    String errorMsg = "Invalid signature. BCB was corrupted.";
                                    throw new Exception(errorMsg);
                                }
                            } catch (Exception ex) {
                                throw new CompletionException(ex);
                            }
                        })
                        .thenAccept((s) -> {
                            result.add(s);
                            if (s != null) counter.getAndIncrement();
                        }).exceptionally((e) -> {
                            System.err.println("Error: " + e.getMessage());
                            return null;
                        }))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());


        while (counter.get() < majority) {
        }

        return result;
    }

    private ArrayList<GeneratedMessageV3> sendAsyncNN(ArrayList<DpasServiceBlockingStub> stubs, String[] command, API api){
        List<GeneratedMessageV3> responses;
        String[] readCommand = {"readGeneral", command[1], "0"};
        responses = readAsync(stubs, readCommand, this::readGeneral, true, null);
        ReadGeneralResponse message = null;
        int maxTs = -2;
        for (GeneratedMessageV3 m : responses) {
            message = (ReadGeneralResponse) m;
            if (maxTs < message.getTs()) {
                maxTs = message.getTs();
            }
        }
        wts = maxTs;
        wts++;

        Announcement requestPost = buildAnnouncement(command);
        ArrayList<BroadcastResponse> bcb = sendBCB(stubs, requestPost);
        if (bcb == null) {
            System.err.println("BCB failed. Command not executed.");
            return null;
        }

        return sendAsync(stubs, command, this::postGeneral, bcb);

    }

    private ArrayList<GeneratedMessageV3> sendAsync(ArrayList<DpasServiceBlockingStub> stubs, String[] command, API api, ArrayList<BroadcastResponse> bcb) {
        AtomicInteger counter = new AtomicInteger(0);
        ArrayList<GeneratedMessageV3> result = new ArrayList<>();
        AtomicBoolean error = new AtomicBoolean(false);
        //For every stub, call grpc call asynchronously
        stubs.stream().map(
                (DpasServiceBlockingStub stub) -> CompletableFuture.supplyAsync(
                        () -> {
                            try {
                                GeneratedMessageV3 tmp = api.grpcOperation(stub, command, bcb);
                                if (tmp == null) error.set(true);
                                return tmp;
                            } catch (Exception ex) {
                                throw new CompletionException(ex);
                            }
                        })
                        .thenAccept((s) -> {
                            //When result arrives, add to return vector and add to counter
                            result.add(s);
                            counter.getAndIncrement();
                        }).exceptionally((e) -> {
                            System.err.println("Server Error: " + e.getMessage());
                            counter.getAndIncrement();
                            return null;
                        }))
                .collect(Collectors.toList());

        //If a majority of answers has been received, return.
        while (counter.get() < majority) {
            if (error.get()) {
                return null;
            }
        }

        return result;
    }

    private ArrayList<GeneratedMessageV3> readAsync(ArrayList<DpasServiceBlockingStub> stubs, String[] command, API api, boolean general, ArrayList<BroadcastResponse> bcb) {
        AtomicInteger counter = new AtomicInteger(0);
        ArrayList<GeneratedMessageV3> result = new ArrayList<>();

        stubs.stream().map(
                (DpasServiceBlockingStub stub) -> CompletableFuture.supplyAsync(
                        () -> {
                            try {
                                GeneratedMessageV3 res = api.grpcOperation(stub, command, bcb);
                                if (verifyWriteSig(res, general)) return res;
                                else {
                                    String message = "Invalid read signature. Read was corrupted.";
                                    throw new Exception(message);
                                }
                            } catch (Exception ex) {
                                throw new CompletionException(ex);
                            }
                        })
                        .thenAccept((s) -> {
                            result.add(s);
                            if (s != null) counter.getAndIncrement();
                        }).exceptionally((e) -> {
                            System.err.println("Error: " + e.getMessage());
                            return null;
                        }))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());


        while (counter.get() < majority) {
        }

        return result;
    }

    public GetTokenResponse getClientToken(DpasServiceBlockingStub stub, String userAlias) throws Exception {
        byte[] keyHash = Main.getHashFromObject(userAlias);
        byte[] keySig = Main.getSignature(keyHash, userAlias);

        GetTokenRequest requestGetToken = GetTokenRequest.newBuilder().setKey(userAlias)
                .setSignature(ByteString.copyFrom(keySig)).build();
        GetTokenResponse responseGetToken = stub.getToken(requestGetToken);
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

    private boolean verifyWriteSig(GeneratedMessageV3 readResponse, boolean general) {
        ArrayList<Announcement> resList;
        if (general) resList = new ArrayList<Announcement>(((ReadGeneralResponse) readResponse).getResultList());
        else resList = new ArrayList<Announcement>(((ReadResponse) readResponse).getResultList());
        if (resList.size() == 0) {
            return true;
        }
        for (Announcement a : resList) {
            try {
                byte[] postHash = Main.getHashFromObject(a.getKey());
                postHash = ArrayUtils.addAll(postHash, Main.getHashFromObject(a.getMessage()));
                byte[] tokenHash = Main.getHashFromObject(a.getToken());
                byte[] wtsHash = Main.getHashFromObject(a.getWts());
                byte[] hash = ArrayUtils.addAll(postHash, tokenHash);
                hash = ArrayUtils.addAll(hash, wtsHash);
                return Main.validate(a.getSignature().toByteArray(), a.getKey(), hash);
            } catch (Exception e) {
                System.err.println(e.getMessage());
                return false;
            }
        }
        return false;
    }

    /*----------------------------------------------------------------------------------------------------------------*/
    /*------------------------------------------------COMMANDS--------------------------------------------------------*/
    /*----------------------------------------------------------------------------------------------------------------*/
    public RegisterResponse register(DpasServiceBlockingStub stub, String[] command, ArrayList<BroadcastResponse> bcb) throws Exception {
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
            return null;
        }

        return responseRegister;
    }

    /*--------------------------------------------------POSTS---------------------------------------------------------*/
    public Announcement buildAnnouncement(String[] command) {
        String userAlias = command[1];
        List<Integer> referral = new ArrayList<Integer>();
        if (command.length > 2) {
            int i = 3;
            while (i < command.length) {
                referral.add(Integer.parseInt(command[i]));
                i += 1;
            }
        }
        Announcement.Builder post = Announcement.newBuilder().setKey(command[1]).setMessage(command[2]).setWts(wts);
        post.addAllRef(referral);
        return post.build();
    }

    public PostResponse post(DpasServiceBlockingStub stub, String[] command, ArrayList<BroadcastResponse> bcb) throws Exception {

        String userAlias = command[1];
        /*-------------------------------GET TOKEN VALIDATION-------------------------------*/
        GetTokenResponse responseGetToken = getClientToken(stub, userAlias);

        boolean tokenVerify = validateToken(responseGetToken);

        if (!tokenVerify) {
            System.err.println("Invalid signature and/or hash. GetToken request corrupted.");
            return null;
        }
        /*----------------------------------------------------------------------------------*/
        String token = responseGetToken.getToken();
        Announcement post = bcb.get(0).getPost(); /*buildAnnouncement(command);*/


        byte[] postHash = Main.getHashFromObject(post.getKey());
        postHash = ArrayUtils.addAll(postHash, Main.getHashFromObject(post.getMessage()));
        byte[] tokenHash = Main.getHashFromObject(token);
        byte[] wtsHash = Main.getHashFromObject(wts);
        byte[] hash = ArrayUtils.addAll(postHash, tokenHash);
        hash = ArrayUtils.addAll(hash, wtsHash);
        byte[] signature = Main.getSignature(hash, userAlias);

        PostRequest requestPost = PostRequest.newBuilder().setPost(post).setSignature(ByteString.copyFrom(signature))
                .setWts(wts).setToken(token).addAllBcb(bcb).build();

        PostResponse responsePost = stub.post(requestPost);

        /*---------------------------------SERVER VALIDATION--------------------------------*/
        ByteString sigServerByteString = responsePost.getSignature();
        String key = responsePost.getResult();

        byte[] resultHash = Main.getHashFromObject(key);

        boolean validResponse = validateServerResponse(sigServerByteString, resultHash);
        if (!validResponse) {
            System.err.println("Invalid signature and/or hash. Post response corrupted.");
            return null;
        }


        return responsePost;
    }


    public PostGeneralResponse postGeneral(DpasServiceBlockingStub stub, String[] command, ArrayList<BroadcastResponse> bcb) throws Exception {
        String userAlias = command[1];
        /*-------------------------------GET TOKEN VALIDATION-------------------------------*/
        GetTokenResponse responseGetToken = getClientToken(stub, userAlias);

        boolean tokenVerify = validateToken(responseGetToken);

        if (!tokenVerify) {
            System.err.println("Invalid signature and/or hash. GetToken request corrupted.");
            return null;
        }
        /*----------------------------------------------------------------------------------*/
        String token = responseGetToken.getToken();
        Announcement post = bcb.get(0).getPost(); /*buildAnnouncement(command);*/

        byte[] postHash = Main.getHashFromObject(post.getKey());
        postHash = ArrayUtils.addAll(postHash, Main.getHashFromObject(post.getMessage()));
        byte[] tokenHash = Main.getHashFromObject(token);
        byte[] wtsHash = Main.getHashFromObject(wts);
        byte[] hash = ArrayUtils.addAll(postHash, tokenHash);
        hash = ArrayUtils.addAll(hash, wtsHash);
        byte[] signature = Main.getSignature(hash, userAlias);

        PostGeneralRequest requestGeneralPost = PostGeneralRequest.newBuilder()
                .setPost(post).setSignature(ByteString.copyFrom(signature)).setToken(token).setWts(wts).addAllBcb(bcb).build();

        PostGeneralResponse responseGeneralPost = stub.postGeneral(requestGeneralPost);
        /*---------------------------------SERVER VALIDATION--------------------------------*/
        ByteString sigServerByteString = responseGeneralPost.getSignature();
        String key = responseGeneralPost.getResult();

        byte[] resultHash = Main.getHashFromObject(key);

        boolean validResponse = validateServerResponse(sigServerByteString, resultHash);
        if (!validResponse) {
            System.err.println("Invalid signature and/or hash. Post General response corrupted.");
            return null;

        }

        return responseGeneralPost;
    }

    /*--------------------------------------------------READS---------------------------------------------------------*/
    public ReadResponse read(DpasServiceBlockingStub stub, String[] command, ArrayList<BroadcastResponse> bcb) throws Exception {
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

        return responseRead;
    }


    public ReadGeneralResponse readGeneral(DpasServiceBlockingStub stub, String[] command, ArrayList<BroadcastResponse> bcb) throws Exception {

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

        return responseReadGeneral;
    }


    public ResetResponse reset(DpasServiceBlockingStub stub, String[] command, ArrayList<BroadcastResponse> bcb) throws Exception {
        ResetRequest resetRequest = ResetRequest.newBuilder().build();
        ResetResponse resetResponse = stub.reset(resetRequest);
        return resetResponse;
    }

    public BroadcastResponse broadcast(DpasServiceBlockingStub stub, Announcement message) throws Exception {
        BroadcastRequest broadcastRequest = BroadcastRequest.newBuilder().setPost(message).build();
        BroadcastResponse broadcastResponse = stub.broadcast(broadcastRequest);
        return broadcastResponse;
    }

}
