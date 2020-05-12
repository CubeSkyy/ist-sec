package com.dpas.client;

import com.dpas.Dpas.*;
import com.dpas.DpasServiceGrpc.DpasServiceBlockingStub;
import com.dpas.crypto.Main;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static com.dpas.client.DpasClient.*;

interface API {
    GeneratedMessageV3 grpcOperation(DpasServiceBlockingStub stub, String[] command, ArrayList<BroadcastResponse> bcb) throws Exception;
}

public class ClientAPI {
    public int wts = 0;
    public int majority;

    public ClientAPI(int numOfServers, int numOfFaults) {
        majority = (int) Math.ceil((numOfServers + numOfFaults) / 2.0);
    }

    //TODO: Add rid to normal signature
    //TODO: Make RID checks
    //TODO: Persist server timestamp?
    //TODO: Change majority to depend on faulty servers

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

    private ArrayList<GeneratedMessageV3> sendAsync(ArrayList<DpasServiceBlockingStub> stubs, String[] command, API api, ArrayList<BroadcastResponse> bcb) {
        List<CompletableFuture<?>> completableFutures =
                stubs.stream().map(stub -> CompletableFuture.supplyAsync(() -> {
                    try {
                        GeneratedMessageV3 tmp = api.grpcOperation(stub, command, bcb);
                        return tmp;
                    } catch (Exception ex) {
                        throw new CompletionException(ex);
                    }
                }).exceptionally(ex -> {
                    System.err.println(ex.getMessage());
                    return null;
                }))
                        .collect(Collectors.toList());

        waitForMajority(completableFutures);

        ArrayList<GeneratedMessageV3> result = (ArrayList<GeneratedMessageV3>) completableFutures.stream()
                .map(completableFuture -> completableFuture.join())
                .filter(Objects::nonNull).collect(Collectors.toList());


        return result;
    }

    private ArrayList<GeneratedMessageV3> sendAsyncNN(ArrayList<DpasServiceBlockingStub> stubs, String[] command, API api) {
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
        ArrayList<BroadcastResponse> bcb = sendBCB(stubs, requestPost, command[1]);
        if (bcb == null) {
            System.err.println("BCB failed. Command not executed.");
            return null;
        }

        return sendAsync(stubs, command, this::postGeneral, bcb);

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

    private void waitForMajority(List<CompletableFuture<?>> completableFutures) {
        while (true) {
            int counter = 0;
            for (CompletableFuture<?> future : completableFutures) {
                if (future.isDone()) counter++;
            }
            if (counter >= majority) break;
        }

    }


    private ArrayList<BroadcastResponse> sendBCB(ArrayList<DpasServiceBlockingStub> stubs, Announcement message, String userAlias) {

        List<CompletableFuture<?>> completableFutures =
                stubs.stream().map(stub -> CompletableFuture.supplyAsync(() -> {
                    try {
                        BroadcastResponse res = broadcast(stub, message, userAlias);
                        byte[] msgHash = Main.getHashFromObject(message);
                        if (validateServerResponse(res.getSignature(), msgHash)) return res;
                        else {
                            String errorMsg = "Invalid signature. BCB was corrupted.";
                            throw new Exception(errorMsg);
                        }
                    } catch (Exception ex) {
                        throw new CompletionException(ex);
                    }

                }).exceptionally(ex -> {
                    System.err.println(ex.getMessage());
                    return null;
                }))
                        .collect(Collectors.toList());

        waitForMajority(completableFutures);

        ArrayList<BroadcastResponse> result = (ArrayList<BroadcastResponse>) completableFutures.stream()
                .map(completableFuture -> completableFuture.join())
                .filter(Objects::nonNull).collect(Collectors.toList());

        return result;
    }


    private ArrayList<GeneratedMessageV3> readAsync(ArrayList<DpasServiceBlockingStub> stubs, String[] command, API api, boolean general, ArrayList<BroadcastResponse> bcb) {
        List<CompletableFuture<?>> completableFutures =
                stubs.stream().map(stub -> CompletableFuture.supplyAsync(() -> {
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
                }).exceptionally(ex -> {
                    System.err.println(ex.getMessage());
                    return null;
                }))
                        .collect(Collectors.toList());

        waitForMajority(completableFutures);

        ArrayList<GeneratedMessageV3> result = (ArrayList<GeneratedMessageV3>) completableFutures.stream()
                .map(completableFuture -> completableFuture.join())
                .filter(Objects::nonNull).collect(Collectors.toList());

        return result;
    }


    public ArrayList<GeneratedMessageV3> receive(ArrayList<DpasServiceBlockingStub> stubs, String input) throws Exception {
        try {
            String[] command = getCommand(input);
            String[] commands;
            ArrayList<GeneratedMessageV3> responses;
            System.out.println("\nCommand: " + command[0] + "\n");
            switch (command[0]) {
                case "register":
                    if (command.length != 2) {
                        System.err.println("Usage: register|<userAlias>");
                        break;
                    }
                    responses = sendAsync(stubs, command, this::register, null);
                    if (responses != null && responses.size() > 0) {
                        RegisterResponse response = (RegisterResponse) responses.get(0);
                        System.out.println("REGISTER COMPLETE: " + response.getResult());
                    }
                    return responses;
                case "post":
                    if (command.length < 3) {
                        System.err.println("Usage: post|<userAlias>|<Message>|<Reference List>\nReference List can be empty.");
                        break;
                    }
                    wts++;
                    Announcement requestPost = buildAnnouncement(command);
                    ArrayList<BroadcastResponse> bcb = sendBCB(stubs, requestPost, command[1]);
                    if (bcb == null) {
                        System.err.println("BCB failed. Command not executed.");
                        break;
                    }

                    responses = sendAsync(stubs, command, this::post, bcb);
                    if (responses != null && responses.size() > 0) {
                        PostResponse response = (PostResponse) responses.get(0);
                        System.out.println("POST DONE: " + response.getResult());
                    }
                    return responses;
                case "postGeneral":
                    if (command.length < 3) {
                        System.err.println("Usage: postGeneral|<userAlias>|<Message>|<Reference List>\nReference List can be empty.");
                        break;
                    }

                    responses = sendAsyncNN(stubs, command, this::postGeneral);
                    if (responses != null && responses.size() > 0) {
                        PostGeneralResponse response = (PostGeneralResponse) responses.get(0);
                        System.out.println("POST GENERAL DONE: " + response.getResult());
                    }

                    return responses;
                case "read":
                    if (command.length != 4) {
                        System.err.println("Usage: read|<userAlias>|<userToRead>|<NumberOfPosts>.");
                        break;
                    }
                    responses = readAsync(stubs, command, this::read, false, null);
                    if (responses != null && responses.size() > 0) {

                        ReadResponse message;
                        ReadResponse result = null;
                        int maxTs = -2;
                        String maxId = ((ReadResponse) responses.get(0)).getResult(0).getKey();
                        for (GeneratedMessageV3 m : responses) {
                            message = (ReadResponse) m;
                            if (maxTs < message.getTs() || (maxTs == message.getTs() && message.getTsId().compareTo(maxId) > 0)) {
                                maxTs = message.getTs();
                                maxId = message.getTsId();
                                result = message;
                            }
                        }
                        System.out.println("READ:");
                        assert result != null;
                        printRead(result.getResultList());
                        ArrayList<GeneratedMessageV3> tmp = new ArrayList<>();
                        tmp.add(result);
                        return tmp;
                    }
                    return null;
                case "readGeneral":
                    if (command.length != 3) {
                        System.err.println("Usage: readGeneral|<userAlias>|<NumberOfPosts>.");
                        break;
                    }
                    responses = readAsync(stubs, command, this::readGeneral, true, null);
                    if (responses != null && responses.size() > 0) {

                        ReadGeneralResponse messageGeneral;
                        ReadGeneralResponse resultGeneral = null;
                        int maxTsGeneral = -2;
                        String maxIdGeneral = ((ReadGeneralResponse) responses.get(0)).getResult(0).getKey();
                        for (GeneratedMessageV3 m : responses) {
                            messageGeneral = (ReadGeneralResponse) m;
                            if (maxTsGeneral < messageGeneral.getTs() || (maxTsGeneral == messageGeneral.getTs() && messageGeneral.getTsId().compareTo(maxIdGeneral) > 0)) {
                                maxTsGeneral = messageGeneral.getTs();
                                maxIdGeneral = messageGeneral.getTsId();
                                resultGeneral = messageGeneral;
                            }
                        }
                        System.out.println("READ GENERAL:");
                        assert resultGeneral != null;
                        printRead(resultGeneral.getResultList());
                        ArrayList<GeneratedMessageV3> tmpGeneral = new ArrayList<>();
                        tmpGeneral.add(resultGeneral);
                        return tmpGeneral;
                    }
                    return null;
                case "reset":
                    for (DpasServiceBlockingStub stub : stubs) {
                        reset(stub, command, null);
                    }
                    System.out.println("RESET DONE.");

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
        return null;
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

        Announcement post = bcb.get(0).getPost();


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
        Announcement post = bcb.get(0).getPost();

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
        byte[] hashTsId = Main.getHashFromObject(responseRead.getTsId());
        byte[] hashTs = Main.getHashFromObject(responseRead.getTs());

        resultHash = ArrayUtils.addAll(resultHash, hashTsId);
        resultHash = ArrayUtils.addAll(resultHash, hashTs);
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
        byte[] hashTsId = Main.getHashFromObject(responseReadGeneral.getTsId());
        byte[] hashTs = Main.getHashFromObject(responseReadGeneral.getTs());

        resultHash = ArrayUtils.addAll(resultHash, hashTsId);
        resultHash = ArrayUtils.addAll(resultHash, hashTs);
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

    public BroadcastResponse broadcast(DpasServiceBlockingStub stub, Announcement message, String userAlias) throws Exception {

        byte[] messageHash = Main.getHashFromObject(message);
        byte[] keyHash = Main.getHashFromObject(userAlias);
        byte[] finalHash = ArrayUtils.addAll(messageHash, keyHash);

        byte[] signature = Main.getSignature(finalHash, userAlias);


        BroadcastRequest broadcastRequest = BroadcastRequest.newBuilder().setPost(message).setKey(userAlias).setSignature(ByteString.copyFrom(signature)).build();
        BroadcastResponse broadcastResponse = stub.broadcast(broadcastRequest);
        return broadcastResponse;
    }

}
