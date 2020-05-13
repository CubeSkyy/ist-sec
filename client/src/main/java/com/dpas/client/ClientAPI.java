package com.dpas.client;

import com.dpas.Dpas.*;
import com.dpas.DpasServiceGrpc.DpasServiceBlockingStub;
import com.dpas.crypto.Main;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static com.dpas.client.DpasClient.*;

@FunctionalInterface
interface API {
    GeneratedMessageV3 grpcOperation(DpasServiceBlockingStub stub, Object command, ArrayList<GeneratedMessageV3> bcb) throws Exception;
}

public class ClientAPI {
    public final int majority;
    private final Comparator<GeneratedMessageV3> byTimeStamp;
    protected int wts = 0;

    public ClientAPI(int numOfServers, int numOfFaults) {
        majority = (int) Math.ceil((numOfServers + numOfFaults) / 2.0);
        byTimeStamp = (GeneratedMessageV3 m1, GeneratedMessageV3 m2) -> {
            Integer ts1 = (Integer) m1.getField(m1.getDescriptorForType().findFieldByName("ts"));
            String tsId1 = (String) m1.getField(m1.getDescriptorForType().findFieldByName("tsId"));
            Integer ts2 = (Integer) m2.getField(m2.getDescriptorForType().findFieldByName("ts"));
            String tsId2 = (String) m2.getField(m2.getDescriptorForType().findFieldByName("tsId"));
            if (ts1.equals(ts2)) {
                return tsId2.compareTo(tsId1);
            }
            return ts1 - ts2;
        };
    }

    public ArrayList<GeneratedMessageV3> receive(ArrayList<DpasServiceBlockingStub> stubs, String input) throws Exception {
        try {
            String[] command = getCommand(input);
            ArrayList<GeneratedMessageV3> responses;
            System.out.println("------------------------------------");
            System.out.println("command: " + command[0] + "\n");
            switch (command[0]) {
                case "register":
                    return registerAPI(stubs, command);
                case "post":
                    return postAPI(stubs, command);
                case "postGeneral":
                    return postGeneralAPI(stubs, command);
                case "read":
                    return readAPI(stubs, command);
                case "readGeneral":
                    return readGeneralAPI(stubs, command);
                case "reset":
                    return resetAPI(stubs);
                case "demo1":
                    //DEMO1 = "reset|register|user1\npost|user1|Test\nread|user1|user1|0";
                    for (String com : DEMO1.split("\n"))
                        receive(stubs, com);
                    break;
                case "demo2":
                    //DEMO2 = "reset|register|user1\nregister|user2\npostGeneral|user1|Test\npostGeneral|user2|Test2|1\nreadGeneral|user1|0";
                    for (String com : DEMO2.split("\n"))
                        receive(stubs, com);
                    break;
                case "demo3":
                    //DEMO3 = "reset|register|user1\npost|user1|Test\npost|user1|Test2\npost|user1|Test3\nread|user1|user1|2";
                    for (String com : DEMO3.split("\n"))
                        receive(stubs, com);
                    break;
                default:
                    System.err.println("Invalid input.");
                    break;
            }
        } catch (
                io.grpc.StatusRuntimeException e) {
            System.err.println(e.getMessage());
        }

        return null;
    }

    /*----------------------------------------------------------------------------------------------------------------*/
    /*----------------------------------------------Async Server Call-------------------------------------------------*/
    /*----------------------------------------------------------------------------------------------------------------*/
    private ArrayList<GeneratedMessageV3> sendAsync(ArrayList<DpasServiceBlockingStub> stubs, Object command, API api, ArrayList<GeneratedMessageV3> bcb) {
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

        return (ArrayList<GeneratedMessageV3>) completableFutures.stream().map(CompletableFuture::join)
                .filter(Objects::nonNull).collect(Collectors.toList());
    }

    /*----------------------------------------------------------------------------------------------------------------*/
    /*--------------------------------------Byzantine Consistent 1--------------------------------------------*/
    /*----------------------------------------------------------------------------------------------------------------*/

    public BroadcastResponse broadcast(DpasServiceBlockingStub stub, Object payload, ArrayList<GeneratedMessageV3> bcb) throws Exception {
        String[] command = (String[]) payload;
        Announcement message = buildAnnouncement(command);
        Object[] obj_list = {message, message.getKey()};
        byte[] signature = Main.getSignatureAll(obj_list, message.getKey());

        BroadcastRequest broadcastRequest = BroadcastRequest.newBuilder().setPost(message).setKey(message.getKey()).setSignature(ByteString.copyFrom(signature)).build();
        BroadcastResponse broadcastResponse = stub.broadcast(broadcastRequest);

        Object[] obj_list2 = {message, broadcastResponse.getKey()};
        if (validateServerResponse(broadcastResponse.getSignature(), obj_list2, broadcastResponse.getKey()))
            return broadcastResponse;
        else {
            String errorMsg = "Invalid signature. BCB was corrupted.";
            throw new Exception(errorMsg);
        }

    }

    public BroadcastRegisterResponse broadcastRegister(DpasServiceBlockingStub stub, Object payload, ArrayList<GeneratedMessageV3> bcb) throws Exception {
        String[] command = (String[]) payload;
        String userAlias = command[1];
        byte[] keyHash = Main.getHashFromObject(userAlias);
        byte[] signature = Main.getSignature(keyHash, userAlias);

        BroadcastRegisterRequest broadcastRegisterRequest = BroadcastRegisterRequest.newBuilder().setUserAlias(userAlias).setSignature(ByteString.copyFrom(signature)).build();
        BroadcastRegisterResponse broadcastResponse = stub.broadcastRegister(broadcastRegisterRequest);
        Object[] obj_list = {userAlias, broadcastResponse.getKey()};
        if (validateServerResponse(broadcastResponse.getSignature(), obj_list, broadcastResponse.getKey()))
            return broadcastResponse;
        else {
            String errorMsg = "Invalid signature. BCB was corrupted.";
            throw new Exception(errorMsg);
        }

    }

    /*----------------------------------------------------------------------------------------------------------------*/
    /*------------------------------------------------Write-Back------------------------------------------------------*/
    /*----------------------------------------------------------------------------------------------------------------*/
    public WriteBackResponse writeBack(DpasServiceBlockingStub stub, Object payload, ArrayList<GeneratedMessageV3> bcb) throws Exception {
        Object[] payload_list = (Object[]) payload;
        ReadResponse posts = (ReadResponse) payload_list[0];
        String userAlias = (String) payload_list[1];

        /*-------------------------------GET TOKEN VALIDATION-------------------------------*/
        GetTokenResponse responseGetToken = getClientToken(stub, userAlias);

        boolean tokenVerify = validateToken(responseGetToken);

        if (!tokenVerify) {
            System.err.println("Invalid signature and/or hash. GetToken request corrupted.");
            return null;
        }
        /*----------------------------------------------------------------------------------*/
        String token = responseGetToken.getToken();
        Object[] obj_list = {posts, userAlias, token};
        byte[] signature = Main.getSignatureAll(obj_list, userAlias);

        WriteBackRequest requestWB = WriteBackRequest.newBuilder().setPosts(posts).setKey(userAlias).setToken(token).setSignature(ByteString.copyFrom(signature)).build();
        WriteBackResponse responseWB = stub.writeBack(requestWB);

        /*---------------------------------SERVER VALIDATION--------------------------------*/
        ByteString sigServerByteString = responseWB.getSignature();
        String key = responseWB.getResult();
        String serverAlias = responseWB.getKey();
        Object[] obj_list2 = {key, serverAlias};
        boolean validResponse = validateServerResponse(sigServerByteString, obj_list2, responseWB.getKey());
        if (!validResponse) {
            System.err.println("Invalid signature and/or hash. Write Back response corrupted.");
            return null;
        }

        return responseWB;
    }

    /*----------------------------------------------------------------------------------------------------------------*/
    /*------------------------------------------------API Commands----------------------------------------------------*/
    /*----------------------------------------------------------------------------------------------------------------*/
    ArrayList<GeneratedMessageV3> registerAPI(ArrayList<DpasServiceBlockingStub> stubs, String[] command) {
        ArrayList<GeneratedMessageV3> responses;

        if (command.length != 2) {
            System.err.println("Usage: register|<userAlias>");
            return null;
        }

        ArrayList<GeneratedMessageV3> bcbRegister = sendAsync(stubs, command, this::broadcastRegister, null);
        if (bcbRegister == null) {
            System.err.println("BCB failed. command not executed.");
            return null;
        }

        responses = sendAsync(stubs, command, this::register, bcbRegister);
        if (responses != null && responses.size() > 0) {
            RegisterResponse response = (RegisterResponse) responses.get(0);
            System.out.println("REGISTER COMPLETE: " + response.getResult());
        }
        return responses;
    }

    ArrayList<GeneratedMessageV3> postAPI(ArrayList<DpasServiceBlockingStub> stubs, String[] command) {
        ArrayList<GeneratedMessageV3> responses;

        if (command.length < 3) {
            System.err.println("Usage: post|<userAlias>|<Message>|<Reference List>\nReference List can be empty.");
            return null;
        }
        wts++;
        ArrayList<GeneratedMessageV3> bcb = sendAsync(stubs, command, this::broadcast, null);
        if (bcb == null) {
            System.err.println("BCB failed. command not executed.");
            return null;
        }

        responses = sendAsync(stubs, command, this::post, bcb);
        if (responses != null && responses.size() > 0) {
            PostResponse response = (PostResponse) responses.get(0);
            System.out.println("POST DONE: " + response.getResult());
        }
        return responses;
    }

    ArrayList<GeneratedMessageV3> postGeneralAPI(ArrayList<DpasServiceBlockingStub> stubs, String[] command) {
        ArrayList<GeneratedMessageV3> responses;

        if (command.length < 3) {
            System.err.println("Usage: postGeneral|<userAlias>|<Message>|<Reference List>\nReference List can be empty.");
            return null;
        }

        String[] readcommand = {"readGeneral", command[1], "0"};
        responses = sendAsync(stubs, readcommand, this::readGeneral, null);
        if (responses.size() > 0) {
            ReadGeneralResponse result = (ReadGeneralResponse) Collections.max(responses, byTimeStamp);
            wts = result.getTs();
            wts++;
        }

        ArrayList<GeneratedMessageV3> bcb = sendAsync(stubs, command, this::broadcast, null);
        if (bcb == null) {
            System.err.println("BCB failed. command not executed.");
            return null;
        }

        responses = sendAsync(stubs, command, this::postGeneral, bcb);
        if (responses != null && responses.size() > 0) {
            PostGeneralResponse response = (PostGeneralResponse) responses.get(0);
            System.out.println("POST GENERAL DONE: " + response.getResult());
        }
        return responses;
    }

    ArrayList<GeneratedMessageV3> readAPI(ArrayList<DpasServiceBlockingStub> stubs, String[] command) {
        ArrayList<GeneratedMessageV3> responses;

        if (command.length != 4) {
            System.err.println("Usage: read|<userAlias>|<userToRead>|<NumberOfPosts>.");
            return null;
        }
        responses = sendAsync(stubs, command, this::read, null);
        if (responses != null && responses.size() > 0) {
            ReadResponse result = (ReadResponse) Collections.max(responses, byTimeStamp);
            Object[] obj_list = {result, command[1]};
            sendAsync(stubs, obj_list, this::writeBack, null);
            System.out.println("READ:");
            assert result != null;
            printRead(result.getResultList());
            ArrayList<GeneratedMessageV3> tmp = new ArrayList<>();
            tmp.add(result);
            return tmp;
        }
        return responses;
    }

    ArrayList<GeneratedMessageV3> readGeneralAPI(ArrayList<DpasServiceBlockingStub> stubs, String[] command) {
        ArrayList<GeneratedMessageV3> responses;

        if (command.length != 3) {
            System.err.println("Usage: readGeneral|<userAlias>|<NumberOfPosts>.");
            return null;
        }
        responses = sendAsync(stubs, command, this::readGeneral, null);
        if (responses != null && responses.size() > 0) {
            ReadGeneralResponse result = (ReadGeneralResponse) Collections.max(responses, byTimeStamp);
            System.out.println("READ GENERAL:");
            assert result != null;
            printRead(result.getResultList());
            ArrayList<GeneratedMessageV3> tmpGeneral = new ArrayList<>();
            tmpGeneral.add(result);
            return tmpGeneral;
        }
        return responses;
    }

    ArrayList<GeneratedMessageV3> resetAPI(ArrayList<DpasServiceBlockingStub> stubs) throws Exception {
        ArrayList<GeneratedMessageV3> responses = new ArrayList<>();
        for (DpasServiceBlockingStub stub : stubs)
            responses.add(reset(stub));
        System.out.println("RESET DONE.");
        return responses;
    }

    /*----------------------------------------------------------------------------------------------------------------*/
    /*------------------------------------------------Server Commands-------------------------------------------------*/
    /*----------------------------------------------------------------------------------------------------------------*/
    public RegisterResponse register(DpasServiceBlockingStub stub, Object payload, ArrayList<GeneratedMessageV3> bcb) throws Exception {
        String[] command = (String[]) payload;
        String userAlias = command[1];
        Object[] obj_list = {userAlias};

        byte[] signature = Main.getSignatureAll(obj_list, userAlias);
        ArrayList<BroadcastRegisterResponse> bcbCast = (ArrayList<BroadcastRegisterResponse>) bcb.stream().map(obj -> (BroadcastRegisterResponse) obj).collect(Collectors.toList());

        RegisterRequest requestRegister = RegisterRequest.newBuilder().setKey(userAlias)
                .setSignature(ByteString.copyFrom(signature)).addAllBcb(bcbCast).build();
        RegisterResponse responseRegister = stub.register(requestRegister);
        /*---------------------------------SERVER VALIDATION--------------------------------*/
        ByteString sigServerByteString = responseRegister.getSignature();
        String key = responseRegister.getResult();
        String serverAlias = responseRegister.getKey();
        Object[] obj_list2 = {key, serverAlias};
        boolean validResponse = validateServerResponse(sigServerByteString, obj_list2, responseRegister.getKey());
        if (!validResponse)
            return null;

        return responseRegister;
    }

    /*--------------------------------------------------POSTS---------------------------------------------------------*/

    public PostResponse post(DpasServiceBlockingStub stub, Object payload, ArrayList<GeneratedMessageV3> bcb) throws Exception {
        String[] command = (String[]) payload;
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
        Announcement post = (Announcement) bcb.get(0).getField(bcb.get(0).getDescriptorForType().findFieldByName("post"));
        ArrayList<BroadcastResponse> bcbCast = (ArrayList<BroadcastResponse>) bcb.stream().map((obj) -> (BroadcastResponse) obj).collect(Collectors.toList());

        Object[] obj_list = {post, bcbCast, token, wts};
        byte[] signature = Main.getSignatureAll(obj_list, userAlias);
        Object[] obj_list2 = {post.getKey(), post.getMessage(), post.getRefList()};
        byte[] announcementSig = Main.getSignatureAll(obj_list2, userAlias);
        PostRequest requestPost = PostRequest.newBuilder().setPost(post).setAnnouncementSig(ByteString.copyFrom(announcementSig)).setSignature(ByteString.copyFrom(signature))
                .setWts(wts).setToken(token).addAllBcb(bcbCast).build();

        PostResponse responsePost = stub.post(requestPost);
        /*---------------------------------SERVER VALIDATION--------------------------------*/
        ByteString sigServerByteString = responsePost.getSignature();
        String key = responsePost.getResult();
        String serverAlias = responsePost.getKey();
        Object[] obj_list3 = {key, serverAlias};

        boolean validResponse = validateServerResponse(sigServerByteString, obj_list3, responsePost.getKey());
        if (!validResponse) {
            System.err.println("Invalid signature and/or hash. Post response corrupted.");
            return null;
        }
        return responsePost;
    }


    public PostGeneralResponse postGeneral(DpasServiceBlockingStub stub, Object payload, ArrayList<GeneratedMessageV3> bcb) throws Exception {
        String[] command = (String[]) payload;
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
        Announcement post = (Announcement) bcb.get(0).getField(bcb.get(0).getDescriptorForType().findFieldByName("post"));
        ArrayList<BroadcastResponse> bcbCast = (ArrayList<BroadcastResponse>) bcb.stream().map((obj) -> (BroadcastResponse) obj).collect(Collectors.toList());
        Object[] obj_list = {post, bcbCast, token, wts};
        byte[] signature = Main.getSignatureAll(obj_list, userAlias);
        Object[] obj_list2 = {post.getKey(), post.getMessage(), post.getRefList()};
        byte[] announcementSig = Main.getSignatureAll(obj_list2, userAlias);
        PostGeneralRequest requestGeneralPost = PostGeneralRequest.newBuilder()
                .setPost(post).setSignature(ByteString.copyFrom(signature)).setAnnouncementSig(ByteString.copyFrom(announcementSig)).setToken(token).setWts(wts).addAllBcb(bcbCast).build();

        PostGeneralResponse responseGeneralPost = stub.postGeneral(requestGeneralPost);
        /*---------------------------------SERVER VALIDATION--------------------------------*/
        ByteString sigServerByteString = responseGeneralPost.getSignature();
        String key = responseGeneralPost.getResult();
        String serverAlias = responseGeneralPost.getKey();
        Object[] obj_list3 = {key, serverAlias};
        boolean validResponse = validateServerResponse(sigServerByteString, obj_list3, responseGeneralPost.getKey());
        if (!validResponse) {
            System.err.println("Invalid signature and/or hash. Post General response corrupted.");
            return null;
        }
        return responseGeneralPost;
    }

    /*--------------------------------------------------READS---------------------------------------------------------*/
    public ReadResponse read(DpasServiceBlockingStub stub, Object payload, ArrayList<GeneratedMessageV3> bcb) throws Exception {
        String[] command = (String[]) payload;
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
        Object[] obj_list = {userAlias, key, number, token};
        byte[] signature = Main.getSignatureAll(obj_list, userAlias);

        ReadRequest requestRead = ReadRequest.newBuilder().setNumber(number).setKey(userAlias)
                .setKeyToRead(key).setSignature(ByteString.copyFrom(signature)).setToken(token).build();

        ReadResponse responseRead = stub.read(requestRead);

        /*---------------------------------SERVER VALIDATION--------------------------------*/
        ByteString sigServerByteString = responseRead.getSignature();
        ArrayList<Announcement> result = new ArrayList<>(responseRead.getResultList());
        String serverAlias = responseRead.getKey();
        Object[] obj_list2 = {result, responseRead.getTsId(), responseRead.getTs(), serverAlias};

        boolean validResponse = validateServerResponse(sigServerByteString, obj_list2, responseRead.getKey());
        if (!validResponse) {
            System.err.println("Invalid signature and/or hash. Read Response corrupted.");
            return null;
        }

        if (verifyWriteSig(responseRead)) return responseRead;
        else {
            String message = "Invalid read signature. Read was corrupted.";
            throw new Exception(message);
        }
    }


    public ReadGeneralResponse readGeneral(DpasServiceBlockingStub stub, Object payload, ArrayList<GeneratedMessageV3> bcb) throws Exception {
        String[] command = (String[]) payload;
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
        Object[] obj_list = {userAlias, number, token};
        byte[] signature = Main.getSignatureAll(obj_list, userAlias);

        ReadGeneralRequest requestReadGeneral = ReadGeneralRequest.newBuilder().setNumber(number)
                .setKey(userAlias).setSignature(ByteString.copyFrom(signature)).setToken(token).build();

        ReadGeneralResponse responseReadGeneral = stub.readGeneral(requestReadGeneral);
        /*---------------------------------SERVER VALIDATION--------------------------------*/
        ByteString sigServerByteString = responseReadGeneral.getSignature();
        ArrayList<Announcement> result = new ArrayList<>(responseReadGeneral.getResultList());
        String serverAlias = responseReadGeneral.getKey();
        Object[] obj_list2 = {result, responseReadGeneral.getTsId(), responseReadGeneral.getTs(), serverAlias};

        boolean validResponse = validateServerResponse(sigServerByteString, obj_list2, responseReadGeneral.getKey());
        if (!validResponse) {
            System.err.println("Invalid signature and/or hash. Read General response corrupted.");
            return null;
        }

        if (verifyWriteSig(responseReadGeneral)) return responseReadGeneral;
        else {
            String message = "Invalid read signature. Read was corrupted.";
            throw new Exception(message);
        }
    }

    public ResetResponse reset(DpasServiceBlockingStub stub) {
        ResetRequest resetRequest = ResetRequest.newBuilder().build();
        return stub.reset(resetRequest);
    }

    /*----------------------------------------------------------------------------------------------------------------*/
    /*--------------------------------------------Helper functions----------------------------------------------------*/
    /*----------------------------------------------------------------------------------------------------------------*/
    public void printRead(List<Announcement> _list) {
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

    protected boolean verifyWriteSig(GeneratedMessageV3 readResponse) {
        ArrayList<Announcement> resList = new ArrayList<>((Collection<? extends Announcement>) readResponse.getField(readResponse.getDescriptorForType().findFieldByName("result")));
        if (resList.size() == 0) {
            return true;
        }
        for (Announcement a : resList) {
            try {
                Object[] obj_list = {a.getKey(), a.getMessage(), a.getRefList()};
                if (!Main.validateFromObjectList(a.getSignature().toByteArray(), obj_list, a.getKey())) return false;
            } catch (Exception e) {
                System.err.println(e.getMessage());
                return false;
            }
        }
        return true;
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


    public String[] getCommand(String command) {
        return command.split("\\|");
    }

    public GetTokenResponse getClientToken(DpasServiceBlockingStub stub, String userAlias) throws Exception {
        Object[] obj_list = {userAlias};
        byte[] keySig = Main.getSignatureAll(obj_list, userAlias);

        GetTokenRequest requestGetToken = GetTokenRequest.newBuilder().setKey(userAlias)
                .setSignature(ByteString.copyFrom(keySig)).build();
        return stub.getToken(requestGetToken);
    }

    public boolean validateToken(GetTokenResponse responseGetToken) throws Exception {
        String token = responseGetToken.getToken();
        String serverAlias = responseGetToken.getKey();

        if (!serverAlias.equals("server1") && !serverAlias.equals("server2") && !serverAlias.equals("server3") && !serverAlias.equals("server4")) {
            System.err.println("Token was not signed by a server.");
            return false;
        }

        Object[] obj_list = {token, serverAlias};
        return Main.validateFromObjectList(responseGetToken.getSignature().toByteArray(), obj_list, serverAlias);
    }

    public boolean validateServerResponse(ByteString signature, Object[] obj_list, String serverAlias) throws Exception {
        byte[] responseSignature = signature.toByteArray();
        if (!serverAlias.equals("server1") && !serverAlias.equals("server2") && !serverAlias.equals("server3") && !serverAlias.equals("server4")) {
            System.err.println("Response not signed by a server.");
            return false;
        }
        return Main.validateFromObjectList(responseSignature, obj_list, serverAlias);
    }

    public Announcement buildAnnouncement(String[] command) {
        String userAlias = command[1];
        List<Integer> referral = new ArrayList<>();

        if (command.length > 2) {
            int i = 3;
            while (i < command.length) {
                referral.add(Integer.parseInt(command[i]));
                i += 1;
            }
        }

        Announcement.Builder post = Announcement.newBuilder().setKey(userAlias).setMessage(command[2]).setWts(wts).setGeneral(command[0].equals("postGeneral"));
        post.addAllRef(referral);
        return post.build();
    }

}
