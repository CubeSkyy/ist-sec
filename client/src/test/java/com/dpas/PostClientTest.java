package com.dpas;

import com.dpas.Dpas.*;
import com.dpas.client.ClientAPI;
import com.dpas.crypto.Main;
import com.dpas.server.ServerDataStore;
import com.google.protobuf.ByteString;
import io.grpc.StatusRuntimeException;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.dpas.ClientDataStore.*;
import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class PostClientTest extends RollbackTestAbstractClass {

    @Test
    public void postValid() throws Exception {
        client.receive(blockingStub, "register|" + CLIENT_TEST_USER);
        boolean postResponse = client.post(blockingStub, client.getCommand("post|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG));
        ReadResponse readResponse = client.read(blockingStub, client.getCommand("read|" + CLIENT_TEST_USER +
                "|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG_NUMBER));

        assertTrue(postResponse);
        assertEquals(CLIENT_TEST_MSG_NUMBER, Integer.toString(readResponse.getResultCount()));
        assertEquals(CLIENT_TEST_USER, readResponse.getResult(0).getKey());
        assertEquals(CLIENT_TEST_MSG, readResponse.getResult(0).getMessage());
        assertEquals(0, readResponse.getResult(0).getRefCount());
        assertEquals(1, readResponse.getResult(0).getPostId());
    }

    @Test
    public void postNonRegistered() throws Exception {
        Throwable exception = assertThrows(StatusRuntimeException.class, () -> {
            client.post(blockingStub, client.getCommand("post|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG));
        });
        assertEquals(ServerDataStore.MSG_ERROR_NOT_REGISTERED, ((StatusRuntimeException) exception).getStatus().getDescription());

    }

    @Test
    public void postInvalidRef() throws Exception {
        client.receive(blockingStub, "register|" + CLIENT_TEST_USER);
        Throwable exception = assertThrows(StatusRuntimeException.class, () -> {
            client.post(blockingStub, client.getCommand("post|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG + "|" + CLIENT_TEST_REF));
        });
        assertEquals(ServerDataStore.MSG_ERROR_INVALID_REF, ((StatusRuntimeException) exception).getStatus().getDescription());
    }

    @Test
    public void post255ValidMsgLen() throws Exception {
        client.receive(blockingStub, "register|" + CLIENT_TEST_USER);
        boolean postResponse = client.post(blockingStub, client.getCommand("post|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_255_MSG));
        ReadResponse readResponse = client.read(blockingStub, client.getCommand("read|" + CLIENT_TEST_USER +
                "|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG_NUMBER));


        assertTrue(postResponse);
        assertEquals(CLIENT_TEST_MSG_NUMBER, Integer.toString(readResponse.getResultCount()));
        assertEquals(CLIENT_TEST_USER, readResponse.getResult(0).getKey());
        assertEquals(CLIENT_TEST_255_MSG, readResponse.getResult(0).getMessage());
        assertEquals(0, readResponse.getResult(0).getRefCount());
        assertEquals(1, readResponse.getResult(0).getPostId());
    }

    @Test
    public void post256InvalidMsgLen() throws Exception {
        client.receive(blockingStub, "register|" + CLIENT_TEST_USER);
        Throwable exception = assertThrows(StatusRuntimeException.class, () -> {
            client.post(blockingStub, client.getCommand("post|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_256_MSG));
        });
        assertEquals(ServerDataStore.MSG_ERROR_POST_MSG_LEN, ((StatusRuntimeException) exception).getStatus().getDescription());
    }


    @Test
    public void postChangeMessageTest() throws Exception {
        changeMessageAPI client = new changeMessageAPI();

        client.receive(blockingStub, "register|" + CLIENT_TEST_USER);
        Throwable exception = assertThrows(StatusRuntimeException.class, () -> {
            client.post(blockingStub, client.getCommand("post|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG));
        });

        assertEquals(ServerDataStore.MSG_ERROR_POST_SIG, ((StatusRuntimeException) exception).getStatus().getDescription());
    }

    @Test
    public void postChangeTokenTest() throws Exception {
        changeTokenAPI client = new changeTokenAPI();

        client.receive(blockingStub, "register|" + CLIENT_TEST_USER);
        Throwable exception = assertThrows(StatusRuntimeException.class, () -> {
            client.post(blockingStub, client.getCommand("post|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG));
        });

        assertEquals(ServerDataStore.MSG_ERROR_POST_SIG, ((StatusRuntimeException) exception).getStatus().getDescription());
    }


    @Test
    public void postReplayTest() throws Exception {
        ReplayAPI client = new ReplayAPI();

        client.receive(blockingStub, "register|" + CLIENT_TEST_USER);
        Throwable exception = assertThrows(StatusRuntimeException.class, () -> {
            client.post(blockingStub, client.getCommand("post|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG));
        });

        assertEquals(ServerDataStore.MSG_ERROR_TOKEN_EXPIRED, ((StatusRuntimeException) exception).getStatus().getDescription());
    }

    @Test
    public void postChangeSignatureTest() throws Exception {
        ChangeSignatureAPI client = new ChangeSignatureAPI();

        client.receive(blockingStub, "register|" + CLIENT_TEST_USER);
        Throwable exception = assertThrows(StatusRuntimeException.class, () -> {
            client.post(blockingStub, client.getCommand("post|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG));
        });

        assertEquals(ServerDataStore.MSG_ERROR_POST_SIG, ((StatusRuntimeException) exception).getStatus().getDescription());
    }


    //TODO: Complete when server has new test user
    @Test
    public void postChangeServerSignatureTest() throws Exception {
//        ManagedChannel inProcessChannelTest;
//        DpasServiceGrpc.DpasServiceBlockingStub blockingStubTest;
//        String serverName = InProcessServerBuilder.generateName();
//
//        grpcCleanupTest.register(InProcessServerBuilder
//                .forName(serverName).directExecutor().addService(serviceImplTest).build().start());
//
//        inProcessChannelTest = grpcCleanupTest.register(
//                InProcessChannelBuilder.forName(serverName).directExecutor().build());
//
//        blockingStubTest = DpasServiceGrpc.newBlockingStub(inProcessChannelTest);
//
//
//        final RegisterResponse[] response = new RegisterResponse[1];
//        Throwable exception = assertThrows(io.grpc.StatusRuntimeException.class, () -> {
//            response[0] = client.register(blockingStub, client.getCommand("register|" + ClientDataStore.TEST_USER));
//        });
//        assertEquals("INVALID_ARGUMENT: Invalid signature and/or hash. Register request denied.", exception.getMessage());
//        assertNull(response[0]);
    }


    private class changeMessageAPI extends ClientAPI {
        @Override
        public boolean post(DpasServiceGrpc.DpasServiceBlockingStub stub, String[] command) throws Exception {

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
            byte[] signature = Main.getSignature(hash, command[1]);

            Announcement.Builder postBuilder = post.toBuilder();
            postBuilder.setMessage(CLIENT_TEST_MSG2);
            post = postBuilder.build();

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
    }

    private class changeTokenAPI extends ClientAPI {
        @Override
        public boolean post(DpasServiceGrpc.DpasServiceBlockingStub stub, String[] command) throws Exception {

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
            byte[] signature = Main.getSignature(hash, command[1]);

            PostRequest requestPost = PostRequest.newBuilder().setPost(post).setSignature(ByteString.copyFrom(signature))
                    .setToken(CLIENT_WRONG_TOKEN).build();
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
    }


    private class ReplayAPI extends ClientAPI {
        @Override
        public boolean post(DpasServiceGrpc.DpasServiceBlockingStub stub, String[] command) throws Exception {

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
            byte[] signature = Main.getSignature(hash, command[1]);

            PostRequest requestPost = PostRequest.newBuilder().setPost(post).setSignature(ByteString.copyFrom(signature))
                    .setToken(token).build();
            PostResponse responsePost = stub.post(requestPost);

            stub.post(requestPost);

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
    }


    private class ChangeSignatureAPI extends ClientAPI {
        @Override
        public boolean post(DpasServiceGrpc.DpasServiceBlockingStub stub, String[] command) throws Exception {

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
            byte[] signature = Main.getSignature(hash, CLIENT_TEST_USER2);

            PostRequest requestPost = PostRequest.newBuilder().setPost(post).setSignature(ByteString.copyFrom(signature))
                    .setToken(token).build();
            PostResponse responsePost = stub.post(requestPost);

            responsePost = stub.post(requestPost);

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
    }


    private class changeResponseTokenTestAPI extends ClientAPI {
        @Override
        public boolean validateToken(GetTokenResponse responseGetToken) throws Exception {
            ByteString serverSigByteString = responseGetToken.getSignature();
            String token = ClientDataStore.CLIENT_WRONG_TOKEN;

            byte[] serverSig = serverSigByteString.toByteArray();
            byte[] tokenHash = Main.getHashFromObject(token);

            boolean valid = Main.validate(serverSig, "server1", tokenHash);
            return valid;
        }
    }

//    private final DpasServiceGrpc.DpasServiceImplBase serviceImplTest =
//            mock(DpasServiceGrpc.DpasServiceImplBase.class, delegatesTo(
//                    new DpasServiceGrpc.DpasServiceImplBase() {
//                        @Override
//                        public synchronized void post(PostRequest request, StreamObserver<PostResponse> responseObserver) {
//                            System.out.println("Post Request Received: " + request);
//
//                            Announcement post = request.getPost();
//                            String key = post.getKey();
//
//                            /*--------------------------SERVER SIGNATURE AND HASH-------------------------------*/
//                            try {
//                                byte[] userHash = Main.getHashFromObject(key);
//                                byte[] sigGeneral = Main.getSignature(userHash, "test1");
//
//                                ByteString responseSigByteString = ByteString.copyFrom(sigGeneral);
//
//                                PostResponse response = PostResponse .newBuilder()
//                                        .setResult(key).setSignature(responseSigByteString).build();
//
//                                responseObserver.onNext(response);
//                                responseObserver.onCompleted();
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }));

}
