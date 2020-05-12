//package com.dpas;
//
//import com.dpas.Dpas.*;
//import com.dpas.client.ClientAPI;
//import com.dpas.crypto.Main;
//import com.dpas.server.ServerDataStore;
//import com.google.protobuf.ByteString;
//import io.grpc.StatusRuntimeException;
//import org.apache.commons.lang3.ArrayUtils;
//import org.apache.commons.lang3.StringUtils;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.junit.runners.JUnit4;
//
//import java.util.ArrayList;
//
//import static com.dpas.ClientDataStore.*;
//import static com.dpas.server.ServerDataStore.*;
//import static org.junit.Assert.*;
//
//@RunWith(JUnit4.class)
//public class PostClientTest extends RollbackTestAbstractClass {
//
//    @Test
//    public void postValid() throws Exception {
//        client.receive(stubs, "register|" + CLIENT_TEST_USER);
//        ArrayList<PostResponse> res = (ArrayList) client.receive(stubs, "post|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG);
//        for (PostResponse ele : res) {
//            assertEquals(ele.getResult(), CLIENT_TEST_USER);
//        }
//        ArrayList<ReadResponse> res2 = (ArrayList) client.receive(stubs, "read|" + CLIENT_TEST_USER +
//                "|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG_NUMBER);
//
//        for (ReadResponse ele : res2) {
//            assertEquals(CLIENT_TEST_MSG_NUMBER, Integer.toString(ele.getResultCount()));
//            assertEquals(CLIENT_TEST_USER, ele.getResult(0).getKey());
//            assertEquals(CLIENT_TEST_MSG, ele.getResult(0).getMessage());
//            assertEquals(0, ele.getResult(0).getRefCount());
//            assertEquals(1, ele.getResult(0).getPostId());
//        }
//
//    }
//
//    @Test
//    public void postNonRegistered() throws Exception {
//
//        ArrayList<RegisterResponse> res = (ArrayList) client.receive(stubs, "post|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG);
//        assertTrue(StringUtils.countMatches(errContent.toString(), MSG_ERROR_NOT_REGISTERED) >= client.majority);
//        assertEquals(0, res.size());
//
//    }
//
//    @Test
//    public void postInvalidRef() throws Exception {
//        client.receive(stubs, "register|" + CLIENT_TEST_USER);
//        ArrayList<PostResponse> res = (ArrayList) client.receive(stubs, "post|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG + "|" + CLIENT_TEST_REF);
//
//        assertTrue(StringUtils.countMatches(errContent.toString(), MSG_ERROR_INVALID_REF) >= client.majority);
//        assertEquals(0, res.size());
//
//    }
//
//    @Test
//    public void post255ValidMsgLen() throws Exception {
//        client.receive(stubs, "register|" + CLIENT_TEST_USER);
//        ArrayList<PostResponse> res = (ArrayList) client.receive(stubs, "post|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG);
//        for (PostResponse ele : res) assertEquals(ele.getResult(), CLIENT_TEST_USER);
//
//        ArrayList<ReadResponse> res2 = (ArrayList) client.receive(stubs, "read|" + CLIENT_TEST_USER +
//                "|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG_NUMBER);
//
//        assertEquals(CLIENT_TEST_MSG_NUMBER, Integer.toString(res2.get(0).getResultCount()));
//        assertEquals(CLIENT_TEST_USER, res2.get(0).getResult(0).getKey());
//        assertEquals(CLIENT_TEST_MSG, res2.get(0).getResult(0).getMessage());
//        assertEquals(0, res2.get(0).getResult(0).getRefCount());
//        assertEquals(1, res2.get(0).getResult(0).getPostId());
//
//
//
//    }
//
//    @Test
//    public void post256InvalidMsgLen() throws Exception {
//        client.receive(stubs, "register|" + CLIENT_TEST_USER);
//        ArrayList<PostResponse> res = (ArrayList) client.receive(stubs, "post|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_256_MSG);
//
//        assertTrue(StringUtils.countMatches(errContent.toString(), MSG_ERROR_POST_MSG_LEN) >= client.majority);
//        assertEquals(0, res.size());
//    }
//
//
//    @Test
//    public void postChangeMessageTest() throws Exception {
//        changeMessageAPI client = new changeMessageAPI(numOfServers, numOfFaults);
//        client.receive(stubs, "register|" + CLIENT_TEST_USER);
//
//        ArrayList<PostResponse> res = (ArrayList) client.receive(stubs, "post|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG);
//
//        assertTrue(StringUtils.countMatches(errContent.toString(), MSG_ERROR_BCB) >= client.majority);
//        assertEquals(0, res.size());
//
//    }
//
//    @Test
//    public void postChangeTokenTest() throws Exception {
//        changeTokenAPI client = new changeTokenAPI(numOfServers, numOfFaults);
//
//        client.receive(stubs, "register|" + CLIENT_TEST_USER);
//
//        ArrayList<PostResponse> res = (ArrayList) client.receive(stubs, "post|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG);
//
//        assertTrue(StringUtils.countMatches(errContent.toString(), MSG_ERROR_BCB) >= client.majority);
//        assertEquals(0, res.size());
//    }
//
//
//    @Test
//    public void postReplayTest() throws Exception {
//        ReplayAPI client = new ReplayAPI(numOfServers, numOfFaults);
//
//
//        client.receive(stubs, "register|" + CLIENT_TEST_USER);
//
//        ArrayList<PostResponse> res = (ArrayList) client.receive(stubs, "post|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG);
//
//        assertTrue(StringUtils.countMatches(errContent.toString(), MSG_ERROR_BCB) >= client.majority);
//        assertEquals(0, res.size());
//
//    }
//
//    @Test
//    public void postChangeSignatureTest() throws Exception {
//        ChangeSignatureAPI client = new ChangeSignatureAPI(numOfServers, numOfFaults);
//
//        client.receive(stubs, "register|" + CLIENT_TEST_USER);
//
//        ArrayList<PostResponse> res = (ArrayList) client.receive(stubs, "post|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG);
//
//        assertTrue(StringUtils.countMatches(errContent.toString(), MSG_ERROR_BCB) >= client.majority);
//        assertEquals(0, res.size());
//
//    }
//
//
//    private class changeMessageAPI extends ClientAPI {
//        public changeMessageAPI(int numOfServers, int numOfFaults) {
//            super(numOfServers, numOfFaults);
//        }
//
//        @Override
//        public PostResponse post(DpasServiceGrpc.DpasServiceBlockingStub stub, String[] command, ArrayList<BroadcastResponse> bcb) throws Exception {
//
//            String userAlias = command[1];
//            /*-------------------------------GET TOKEN VALIDATION-------------------------------*/
//            GetTokenResponse responseGetToken = getClientToken(stub, userAlias);
//
//            boolean tokenVerify = validateToken(responseGetToken);
//
//            if (!tokenVerify) {
//                System.err.println("Invalid signature and/or hash. GetToken request corrupted.");
//                return null;
//            }
//            /*----------------------------------------------------------------------------------*/
//            String token = responseGetToken.getToken();
//            Announcement post = buildAnnouncement(command);
//
//
//            byte[] postHash = Main.getHashFromObject(post.getKey());
//            postHash = ArrayUtils.addAll(postHash, Main.getHashFromObject(post.getMessage()));
//            byte[] tokenHash = Main.getHashFromObject(token);
//            byte[] wtsHash = Main.getHashFromObject(wts);
//            byte[] hash = ArrayUtils.addAll(postHash, tokenHash);
//            hash = ArrayUtils.addAll(hash, wtsHash);
//            byte[] signature = Main.getSignature(hash, userAlias);
//
//            Announcement.Builder postBuilder = post.toBuilder();
//            postBuilder.setMessage(CLIENT_TEST_MSG2);
//            post = postBuilder.build();
//
//            PostRequest requestPost = PostRequest.newBuilder().setPost(post).setSignature(ByteString.copyFrom(signature))
//                    .setWts(wts).setToken(token).build();
//            PostResponse responsePost = stub.post(requestPost);
//
//            /*---------------------------------SERVER VALIDATION--------------------------------*/
//            ByteString sigServerByteString = responsePost.getSignature();
//            String key = responsePost.getResult();
//
//            byte[] resultHash = Main.getHashFromObject(key);
//
//            boolean validResponse = validateServerResponse(sigServerByteString, resultHash);
//            if (!validResponse) {
//                System.err.println("Invalid signature and/or hash. Post response corrupted.");
//                return null;
//            }
//
//
//            return responsePost;
//        }
//
//
//    }
//
//    private class changeTokenAPI extends ClientAPI {
//        public changeTokenAPI(int numOfServers, int numOfFaults) {
//            super(numOfServers, numOfFaults);
//        }
//
//        @Override
//        public PostResponse post(DpasServiceGrpc.DpasServiceBlockingStub stub, String[] command, ArrayList<BroadcastResponse> bcb) throws Exception {
//
//            String userAlias = command[1];
//            /*-------------------------------GET TOKEN VALIDATION-------------------------------*/
//            GetTokenResponse responseGetToken = getClientToken(stub, userAlias);
//
//            boolean tokenVerify = validateToken(responseGetToken);
//
//            if (!tokenVerify) {
//                System.err.println("Invalid signature and/or hash. GetToken request corrupted.");
//                return null;
//            }
//            /*----------------------------------------------------------------------------------*/
//            String token = responseGetToken.getToken();
//            Announcement post = bcb.get(0).getPost();
//
//
//            byte[] postHash = Main.getHashFromObject(post.getKey());
//            postHash = ArrayUtils.addAll(postHash, Main.getHashFromObject(post.getMessage()));
//            byte[] tokenHash = Main.getHashFromObject(token);
//            byte[] wtsHash = Main.getHashFromObject(wts);
//            byte[] hash = ArrayUtils.addAll(postHash, tokenHash);
//            hash = ArrayUtils.addAll(hash, wtsHash);
//            byte[] signature = Main.getSignature(hash, userAlias);
//
//            PostRequest requestPost = PostRequest.newBuilder().setPost(post).setSignature(ByteString.copyFrom(signature))
//                    .setWts(wts).setToken(CLIENT_WRONG_TOKEN).build();
//            PostResponse responsePost = stub.post(requestPost);
//
//            /*---------------------------------SERVER VALIDATION--------------------------------*/
//            ByteString sigServerByteString = responsePost.getSignature();
//            String key = responsePost.getResult();
//
//            byte[] resultHash = Main.getHashFromObject(key);
//
//            boolean validResponse = validateServerResponse(sigServerByteString, resultHash);
//            if (!validResponse) {
//                System.err.println("Invalid signature and/or hash. Post response corrupted.");
//                return null;
//            }
//
//
//            return responsePost;
//        }
//
//
//    }
//
//
//    private class ReplayAPI extends ClientAPI {
//        public ReplayAPI(int numOfServers, int numOfFaults) {
//            super(numOfServers, numOfFaults);
//        }
//
//        @Override
//        public PostResponse post(DpasServiceGrpc.DpasServiceBlockingStub stub, String[] command, ArrayList<BroadcastResponse> bcb) throws Exception {
//
//            String userAlias = command[1];
//            /*-------------------------------GET TOKEN VALIDATION-------------------------------*/
//            GetTokenResponse responseGetToken = getClientToken(stub, userAlias);
//
//            boolean tokenVerify = validateToken(responseGetToken);
//
//            if (!tokenVerify) {
//                System.err.println("Invalid signature and/or hash. GetToken request corrupted.");
//                return null;
//            }
//            /*----------------------------------------------------------------------------------*/
//            String token = responseGetToken.getToken();
//            Announcement post = bcb.get(0).getPost();
//
//
//            byte[] postHash = Main.getHashFromObject(post.getKey());
//            postHash = ArrayUtils.addAll(postHash, Main.getHashFromObject(post.getMessage()));
//            byte[] tokenHash = Main.getHashFromObject(token);
//            byte[] wtsHash = Main.getHashFromObject(wts);
//            byte[] hash = ArrayUtils.addAll(postHash, tokenHash);
//            hash = ArrayUtils.addAll(hash, wtsHash);
//            byte[] signature = Main.getSignature(hash, userAlias);
//
//            PostRequest requestPost = PostRequest.newBuilder().setPost(post).setSignature(ByteString.copyFrom(signature))
//                    .setWts(wts).setToken(token).build();
//            PostResponse responsePost = stub.post(requestPost);
//            stub.post(requestPost);
//
//            /*---------------------------------SERVER VALIDATION--------------------------------*/
//            ByteString sigServerByteString = responsePost.getSignature();
//            String key = responsePost.getResult();
//
//            byte[] resultHash = Main.getHashFromObject(key);
//
//            boolean validResponse = validateServerResponse(sigServerByteString, resultHash);
//            if (!validResponse) {
//                System.err.println("Invalid signature and/or hash. Post response corrupted.");
//                return null;
//            }
//
//
//            return responsePost;
//        }
//    }
//
//
//    private class ChangeSignatureAPI extends ClientAPI {
//        public ChangeSignatureAPI(int numOfServers, int numOfFaults) {
//            super(numOfServers, numOfFaults);
//        }
//
//        @Override
//        public PostResponse post(DpasServiceGrpc.DpasServiceBlockingStub stub, String[] command, ArrayList<BroadcastResponse> bcb) throws Exception {
//
//            String userAlias = command[1];
//            /*-------------------------------GET TOKEN VALIDATION-------------------------------*/
//            GetTokenResponse responseGetToken = getClientToken(stub, userAlias);
//
//            boolean tokenVerify = validateToken(responseGetToken);
//
//            if (!tokenVerify) {
//                System.err.println("Invalid signature and/or hash. GetToken request corrupted.");
//                return null;
//            }
//            /*----------------------------------------------------------------------------------*/
//            String token = responseGetToken.getToken();
//            Announcement post = bcb.get(0).getPost();
//
//
//            byte[] postHash = Main.getHashFromObject(post.getKey());
//            postHash = ArrayUtils.addAll(postHash, Main.getHashFromObject(post.getMessage()));
//            byte[] tokenHash = Main.getHashFromObject(token);
//            byte[] wtsHash = Main.getHashFromObject(wts);
//            byte[] hash = ArrayUtils.addAll(postHash, tokenHash);
//            hash = ArrayUtils.addAll(hash, wtsHash);
//            byte[] signature = Main.getSignature(hash, CLIENT_TEST_USER2);
//
//            PostRequest requestPost = PostRequest.newBuilder().setPost(post).setSignature(ByteString.copyFrom(signature))
//                    .setWts(wts).setToken(token).build();
//            PostResponse responsePost = stub.post(requestPost);
//
//            /*---------------------------------SERVER VALIDATION--------------------------------*/
//            ByteString sigServerByteString = responsePost.getSignature();
//            String key = responsePost.getResult();
//
//            byte[] resultHash = Main.getHashFromObject(key);
//
//            boolean validResponse = validateServerResponse(sigServerByteString, resultHash);
//            if (!validResponse) {
//                System.err.println("Invalid signature and/or hash. Post response corrupted.");
//                return null;
//            }
//
//
//            return responsePost;
//        }
//
//    }
//
//}
