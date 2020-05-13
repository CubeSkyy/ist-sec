package com.dpas;

import com.dpas.Dpas.*;
import com.dpas.client.ClientAPI;
import com.dpas.crypto.Main;
import com.dpas.server.ServerDataStore;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;
import io.grpc.StatusRuntimeException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.stream.Collectors;

import static com.dpas.ClientDataStore.*;
import static com.dpas.server.ServerDataStore.*;
import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class PostClientTest extends RollbackTestAbstractClass {

    @Test
    public void postValid() throws Exception {
        client.receive(stubs, "register|" + CLIENT_TEST_USER);
        ArrayList<PostResponse> res = (ArrayList) client.receive(stubs, "post|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG);
        for (PostResponse ele : res) {
            assertEquals(ele.getResult(), CLIENT_TEST_USER);
        }
        ArrayList<ReadResponse> res2 = (ArrayList) client.receive(stubs, "read|" + CLIENT_TEST_USER +
                "|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG_NUMBER);

        for (ReadResponse ele : res2) {
            assertEquals(CLIENT_TEST_MSG_NUMBER, Integer.toString(ele.getResultCount()));
            assertEquals(CLIENT_TEST_USER, ele.getResult(0).getKey());
            assertEquals(CLIENT_TEST_MSG, ele.getResult(0).getMessage());
            assertEquals(0, ele.getResult(0).getRefCount());
            assertEquals(1, ele.getResult(0).getPostId());
        }

    }

    @Test
    public void postNonRegistered() throws Exception {

        ArrayList<RegisterResponse> res = (ArrayList) client.receive(stubs, "post|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG);
        assertTrue(StringUtils.countMatches(errContent.toString(), MSG_ERROR_NOT_REGISTERED) >= client.majority);
        assertEquals(0, res.size());

    }

    @Test
    public void postInvalidRef() throws Exception {
        client.receive(stubs, "register|" + CLIENT_TEST_USER);
        ArrayList<PostResponse> res = (ArrayList) client.receive(stubs, "post|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG + "|" + CLIENT_TEST_REF);

        assertTrue(StringUtils.countMatches(errContent.toString(), MSG_ERROR_INVALID_REF) >= client.majority);
        assertEquals(0, res.size());

    }

    @Test
    public void post255ValidMsgLen() throws Exception {
        client.receive(stubs, "register|" + CLIENT_TEST_USER);
        ArrayList<PostResponse> res = (ArrayList) client.receive(stubs, "post|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_255_MSG);
        for (PostResponse ele : res) assertEquals(ele.getResult(), CLIENT_TEST_USER);

        ArrayList<ReadResponse> res2 = (ArrayList) client.receive(stubs, "read|" + CLIENT_TEST_USER +
                "|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG_NUMBER);

        assertEquals(CLIENT_TEST_MSG_NUMBER, Integer.toString(res2.get(0).getResultCount()));
        assertEquals(CLIENT_TEST_USER, res2.get(0).getResult(0).getKey());
        assertEquals(CLIENT_TEST_255_MSG, res2.get(0).getResult(0).getMessage());
        assertEquals(0, res2.get(0).getResult(0).getRefCount());
        assertEquals(1, res2.get(0).getResult(0).getPostId());



    }

    @Test
    public void post256InvalidMsgLen() throws Exception {
        client.receive(stubs, "register|" + CLIENT_TEST_USER);
        ArrayList<PostResponse> res = (ArrayList) client.receive(stubs, "post|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_256_MSG);

        assertTrue(StringUtils.countMatches(errContent.toString(), MSG_ERROR_POST_MSG_LEN) >= client.majority);
        assertEquals(0, res.size());
    }


    @Test
    public void postChangeMessageTest() throws Exception {
        changeMessageAPI client = new changeMessageAPI(numOfServers, numOfFaults);
        client.receive(stubs, "register|" + CLIENT_TEST_USER);

        ArrayList<PostResponse> res = (ArrayList) client.receive(stubs, "post|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG);

        assertTrue(StringUtils.countMatches(errContent.toString(), MSG_ERROR_BCB) >= client.majority);
        assertEquals(0, res.size());

    }

    @Test
    public void postChangeTokenTest() throws Exception {
        changeTokenAPI client = new changeTokenAPI(numOfServers, numOfFaults);

        client.receive(stubs, "register|" + CLIENT_TEST_USER);

        ArrayList<PostResponse> res = (ArrayList) client.receive(stubs, "post|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG);

        assertTrue(StringUtils.countMatches(errContent.toString(), MSG_ERROR_POST_SIG) >= client.majority);
        assertEquals(0, res.size());
    }


    @Test
    public void postReplayTest() throws Exception {
        ReplayAPI client = new ReplayAPI(numOfServers, numOfFaults);


        client.receive(stubs, "register|" + CLIENT_TEST_USER);

        ArrayList<PostResponse> res = (ArrayList) client.receive(stubs, "post|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG);

        assertTrue(StringUtils.countMatches(errContent.toString(), MSG_ERROR_TOKEN_EXPIRED) >= client.majority);
        assertEquals(0, res.size());

    }

    @Test
    public void postChangeSignatureTest() throws Exception {
        ChangeSignatureAPI client = new ChangeSignatureAPI(numOfServers, numOfFaults);

        client.receive(stubs, "register|" + CLIENT_TEST_USER);

        ArrayList<PostResponse> res = (ArrayList) client.receive(stubs, "post|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG);

        assertTrue(StringUtils.countMatches(errContent.toString(), MSG_ERROR_POST_SIG) >= client.majority);
        assertEquals(0, res.size());

    }


    private class changeMessageAPI extends ClientAPI {
        public changeMessageAPI(int numOfServers, int numOfFaults) {
            super(numOfServers, numOfFaults);
        }

        @Override
        public PostResponse post(DpasServiceGrpc.DpasServiceBlockingStub stub, Object payload, ArrayList<GeneratedMessageV3> bcb) throws Exception {
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

            Object[] obj_list = {post.getKey(), post.getMessage(), token, wts};
            byte[] signature = Main.getSignatureAll(obj_list, userAlias);
            Announcement.Builder postBuilder = post.toBuilder();
            postBuilder.setMessage(CLIENT_TEST_MSG2);
            post = postBuilder.build();
            ArrayList<BroadcastResponse> bcbCast = (ArrayList<BroadcastResponse>) bcb.stream().map(obj -> (BroadcastResponse) obj).collect(Collectors.toList());

            PostRequest requestPost = PostRequest.newBuilder().setPost(post).setSignature(ByteString.copyFrom(signature))
                    .setWts(wts).setToken(token).addAllBcb(bcbCast).build();

            PostResponse responsePost = stub.post(requestPost);

            /*---------------------------------SERVER VALIDATION--------------------------------*/
            ByteString sigServerByteString = responsePost.getSignature();
            String key = responsePost.getResult();
            String serverAlias = responsePost.getKey();

            Object[] obj_list2 = {key, serverAlias};

            boolean validResponse = validateServerResponse(sigServerByteString, obj_list2, responsePost.getKey());
            if (!validResponse) {
                System.err.println("Invalid signature and/or hash. Post response corrupted.");
                return null;
            }
            return responsePost;
        }
    }

    private class changeTokenAPI extends ClientAPI {
        public changeTokenAPI(int numOfServers, int numOfFaults) {
            super(numOfServers, numOfFaults);
        }

        @Override
        public PostResponse post(DpasServiceGrpc.DpasServiceBlockingStub stub, Object payload, ArrayList<GeneratedMessageV3> bcb) throws Exception {
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

            Object[] obj_list = {post.getKey(), post.getMessage(), token, wts};
            byte[] signature = Main.getSignatureAll(obj_list, userAlias);

            ArrayList<BroadcastResponse> bcbCast = (ArrayList<BroadcastResponse>) bcb.stream().map(obj -> (BroadcastResponse) obj).collect(Collectors.toList());

            PostRequest requestPost = PostRequest.newBuilder().setPost(post).setSignature(ByteString.copyFrom(signature))
                    .setWts(wts).setToken(CLIENT_WRONG_TOKEN).addAllBcb(bcbCast).build();

            PostResponse responsePost = stub.post(requestPost);

            /*---------------------------------SERVER VALIDATION--------------------------------*/
            ByteString sigServerByteString = responsePost.getSignature();
            String key = responsePost.getResult();
            String serverAlias = responsePost.getKey();

            Object[] obj_list2 = {key, serverAlias};

            boolean validResponse = validateServerResponse(sigServerByteString, obj_list2, responsePost.getKey());
            if (!validResponse) {
                System.err.println("Invalid signature and/or hash. Post response corrupted.");
                return null;
            }
            return responsePost;
        }
    }


    private class ReplayAPI extends ClientAPI {
        public ReplayAPI(int numOfServers, int numOfFaults) {
            super(numOfServers, numOfFaults);
        }

        @Override
        public PostResponse post(DpasServiceGrpc.DpasServiceBlockingStub stub, Object payload, ArrayList<GeneratedMessageV3> bcb) throws Exception {
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

            Object[] obj_list = {post.getKey(), post.getMessage(), token, wts};
            byte[] signature = Main.getSignatureAll(obj_list, userAlias);

            ArrayList<BroadcastResponse> bcbCast = (ArrayList<BroadcastResponse>) bcb.stream().map(obj -> (BroadcastResponse) obj).collect(Collectors.toList());

            PostRequest requestPost = PostRequest.newBuilder().setPost(post).setSignature(ByteString.copyFrom(signature))
                    .setWts(wts).setToken(token).addAllBcb(bcbCast).build();

            PostResponse responsePost = stub.post(requestPost);
            stub.post(requestPost);
            /*---------------------------------SERVER VALIDATION--------------------------------*/
            ByteString sigServerByteString = responsePost.getSignature();
            String key = responsePost.getResult();
            String serverAlias = responsePost.getKey();

            Object[] obj_list2 = {key, serverAlias};

            boolean validResponse = validateServerResponse(sigServerByteString, obj_list2, responsePost.getKey());
            if (!validResponse) {
                System.err.println("Invalid signature and/or hash. Post response corrupted.");
                return null;
            }

            return responsePost;
        }
    }


    private class ChangeSignatureAPI extends ClientAPI {
        public ChangeSignatureAPI(int numOfServers, int numOfFaults) {
            super(numOfServers, numOfFaults);
        }

        @Override
        public PostResponse post(DpasServiceGrpc.DpasServiceBlockingStub stub, Object payload, ArrayList<GeneratedMessageV3> bcb) throws Exception {
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

            Object[] obj_list = {post.getKey(), post.getMessage(), token, wts};
            byte[] signature = Main.getSignatureAll(obj_list, CLIENT_TEST_USER2);

            ArrayList<BroadcastResponse> bcbCast = (ArrayList<BroadcastResponse>) bcb.stream().map(obj -> (BroadcastResponse) obj).collect(Collectors.toList());

            PostRequest requestPost = PostRequest.newBuilder().setPost(post).setSignature(ByteString.copyFrom(signature))
                    .setWts(wts).setToken(token).addAllBcb(bcbCast).build();

            PostResponse responsePost = stub.post(requestPost);

            /*---------------------------------SERVER VALIDATION--------------------------------*/
            ByteString sigServerByteString = responsePost.getSignature();
            String key = responsePost.getResult();
            String serverAlias = responsePost.getKey();

            Object[] obj_list2 = {key, serverAlias};

            boolean validResponse = validateServerResponse(sigServerByteString, obj_list2, responsePost.getKey());
            if (!validResponse) {
                System.err.println("Invalid signature and/or hash. Post response corrupted.");
                return null;
            }

            return responsePost;
        }

    }

}
