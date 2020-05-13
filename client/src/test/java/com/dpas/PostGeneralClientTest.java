package com.dpas;

import com.dpas.Dpas.*;
import com.dpas.client.ClientAPI;
import com.dpas.crypto.Main;
import com.dpas.server.ServerDataStore;
import com.google.protobuf.ByteString;
import io.grpc.StatusRuntimeException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;

import static com.dpas.ClientDataStore.*;
import static com.dpas.server.ServerDataStore.*;
import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class PostGeneralClientTest extends RollbackTestAbstractClass {

    @Test
    public void postGeneralValid() throws Exception {
        client.receive(stubs, "register|" + CLIENT_TEST_USER);
        ArrayList<PostGeneralResponse> res = (ArrayList) client.receive(stubs, "postGeneral|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG2);
        for (PostGeneralResponse ele : res) {
            assertEquals(ele.getResult(), CLIENT_TEST_USER);
        }
        ArrayList<ReadGeneralResponse> res2 = (ArrayList) client.receive(stubs, "readGeneral|" + CLIENT_TEST_USER
                + "|" + 0);

        for (ReadGeneralResponse ele : res2) {
            assertEquals(CLIENT_TEST_MSG_NUMBER, Integer.toString(ele.getResultCount()));
            assertEquals(CLIENT_TEST_USER, ele.getResult(0).getKey());
            assertEquals(CLIENT_TEST_MSG2, ele.getResult(0).getMessage());
            assertEquals(0, ele.getResult(0).getRefCount());
            assertEquals(1, ele.getResult(0).getPostId());
        }


    }

    @Test
    public void postGeneralNonRegistered() throws Exception {

        ArrayList<PostGeneralResponse> res = (ArrayList) client.receive(stubs, "postGeneral|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG);
        assertTrue(StringUtils.countMatches(errContent.toString(), MSG_ERROR_NOT_REGISTERED) >= client.majority);
        assertEquals(0, res.size());

    }

    @Test
    public void postGeneralInvalidRef() throws Exception {
        client.receive(stubs, "register|" + CLIENT_TEST_USER);

        ArrayList<PostGeneralResponse> res = (ArrayList) client.receive(stubs,"postGeneral|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG + "|" + CLIENT_TEST_REF);
        assertTrue(StringUtils.countMatches(errContent.toString(), MSG_ERROR_INVALID_REF) >= client.majority);
        assertEquals(0, res.size());


    }

    @Test
    public void postGeneral255ValidMsgLen() throws Exception {

        client.receive(stubs, "register|" + CLIENT_TEST_USER);
        ArrayList<PostGeneralResponse> res = (ArrayList) client.receive(stubs, "postGeneral|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_255_MSG);
        for (PostGeneralResponse ele : res) assertEquals(ele.getResult(), CLIENT_TEST_USER);

        ArrayList<ReadGeneralResponse> res2 = (ArrayList) client.receive(stubs, "readGeneral|" + CLIENT_TEST_USER +
                "|" + CLIENT_TEST_MSG_NUMBER);

        assertEquals(CLIENT_TEST_MSG_NUMBER, Integer.toString(res2.get(0).getResultCount()));
        assertEquals(CLIENT_TEST_USER, res2.get(0).getResult(0).getKey());
        assertEquals(CLIENT_TEST_255_MSG, res2.get(0).getResult(0).getMessage());
        assertEquals(0, res2.get(0).getResult(0).getRefCount());
        assertEquals(1, res2.get(0).getResult(0).getPostId());

    }

    @Test
    public void postGeneral256InvalidMsgLen() throws Exception {
        client.receive(stubs, "register|" + CLIENT_TEST_USER);
        ArrayList<PostResponse> res = (ArrayList) client.receive(stubs, "postGeneral|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_256_MSG);

        assertTrue(StringUtils.countMatches(errContent.toString(), MSG_ERROR_POST_MSG_LEN) >= client.majority);
        assertEquals(0, res.size());
    }


    @Test
    public void postGeneralChangeMessageTest() throws Exception {
        changeMessageAPI client = new changeMessageAPI(numOfServers, numOfFaults);
        client.receive(stubs, "register|" + CLIENT_TEST_USER);

        ArrayList<PostGeneralResponse> res = (ArrayList) client.receive(stubs, "postGeneral|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG);

        assertTrue(StringUtils.countMatches(errContent.toString(), MSG_ERROR_BCB) >= client.majority);
        assertEquals(0, res.size());
    }

    @Test
    public void postGeneralChangeTokenTest() throws Exception {
        changeTokenAPI client = new changeTokenAPI(numOfServers, numOfFaults);

        client.receive(stubs, "register|" + CLIENT_TEST_USER);

        ArrayList<PostGeneralResponse> res = (ArrayList) client.receive(stubs, "postGeneral|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG);

        assertTrue(StringUtils.countMatches(errContent.toString(), MSG_ERROR_BCB) >= client.majority);
        assertEquals(0, res.size());
    }


    @Test
    public void postGeneralReplayTest() throws Exception {
        ReplayAPI client = new ReplayAPI(numOfServers, numOfFaults);

        client.receive(stubs, "register|" + CLIENT_TEST_USER);

        ArrayList<PostGeneralResponse> res = (ArrayList) client.receive(stubs, "postGeneral|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG);

        assertTrue(StringUtils.countMatches(errContent.toString(), MSG_ERROR_BCB) >= client.majority);
        assertEquals(0, res.size());

    }

    @Test
    public void postGeneralChangeSignatureTest() throws Exception {
        ChangeSignatureAPI client = new ChangeSignatureAPI(numOfServers, numOfFaults);

        client.receive(stubs, "register|" + CLIENT_TEST_USER);

        ArrayList<PostGeneralResponse> res = (ArrayList) client.receive(stubs, "postGeneral|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG);

        assertTrue(StringUtils.countMatches(errContent.toString(), MSG_ERROR_BCB) >= client.majority);
        assertEquals(0, res.size());

    }


    private class changeMessageAPI extends ClientAPI {
        public changeMessageAPI(int numOfServers, int numOfFaults) {
            super(numOfServers, numOfFaults);
        }


        @Override
        public PostGeneralResponse postGeneral(DpasServiceGrpc.DpasServiceBlockingStub stub, String[] command, ArrayList<BroadcastResponse> bcb) throws Exception {
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
            Announcement.Builder postBuilder = post.toBuilder();
            postBuilder.setMessage(CLIENT_TEST_MSG2);
            post = postBuilder.build();

            PostGeneralRequest requestGeneralPost = PostGeneralRequest.newBuilder()
                    .setPost(post).setSignature(ByteString.copyFrom(signature)).setToken(token).setWts(wts).build();

            PostGeneralResponse responseGeneralPost = stub.postGeneral(requestGeneralPost);
            /*---------------------------------SERVER VALIDATION--------------------------------*/
            ByteString sigServerByteString = responseGeneralPost.getSignature();
            String key = responseGeneralPost.getResult();

            byte[] resultHash = Main.getHashFromObject(key);

            boolean validResponse = validateServerResponse(sigServerByteString, resultHash, responseGeneralPost.getKey());
            if (!validResponse) {
                System.err.println("Invalid signature and/or hash. Post General response corrupted.");
                return null;

            }

            return responseGeneralPost;
        }

    }

    private class changeTokenAPI extends ClientAPI {
        public changeTokenAPI(int numOfServers, int numOfFaults) {
            super(numOfServers, numOfFaults);
        }

        @Override
        public PostGeneralResponse postGeneral(DpasServiceGrpc.DpasServiceBlockingStub stub, String[] command, ArrayList<BroadcastResponse> bcb) throws Exception {
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
                    .setPost(post).setSignature(ByteString.copyFrom(signature)).setToken(CLIENT_WRONG_TOKEN).setWts(wts).build();

            PostGeneralResponse responseGeneralPost = stub.postGeneral(requestGeneralPost);
            /*---------------------------------SERVER VALIDATION--------------------------------*/
            ByteString sigServerByteString = responseGeneralPost.getSignature();
            String key = responseGeneralPost.getResult();

            byte[] resultHash = Main.getHashFromObject(key);

            boolean validResponse = validateServerResponse(sigServerByteString, resultHash, responseGeneralPost.getKey());
            if (!validResponse) {
                System.err.println("Invalid signature and/or hash. Post General response corrupted.");
                return null;

            }

            return responseGeneralPost;
        }

    }


    private class ReplayAPI extends ClientAPI {
        public ReplayAPI(int numOfServers, int numOfFaults) {
            super(numOfServers, numOfFaults);
        }
        public PostGeneralResponse postGeneral(DpasServiceGrpc.DpasServiceBlockingStub stub, String[] command, ArrayList<BroadcastResponse> bcb) throws Exception {
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
                    .setPost(post).setSignature(ByteString.copyFrom(signature)).setToken(token).setWts(wts).build();

            PostGeneralResponse responseGeneralPost = stub.postGeneral(requestGeneralPost);
            stub.postGeneral(requestGeneralPost);
            /*---------------------------------SERVER VALIDATION--------------------------------*/
            ByteString sigServerByteString = responseGeneralPost.getSignature();
            String key = responseGeneralPost.getResult();

            byte[] resultHash = Main.getHashFromObject(key);

            boolean validResponse = validateServerResponse(sigServerByteString, resultHash, responseGeneralPost.getKey());
            if (!validResponse) {
                System.err.println("Invalid signature and/or hash. Post General response corrupted.");
                return null;

            }

            return responseGeneralPost;
        }
    }


    private class ChangeSignatureAPI extends ClientAPI {
        public ChangeSignatureAPI(int numOfServers, int numOfFaults) {
            super(numOfServers, numOfFaults);
        }

        @Override
        public PostGeneralResponse postGeneral(DpasServiceGrpc.DpasServiceBlockingStub stub, String[] command, ArrayList<BroadcastResponse> bcb) throws Exception {
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
            byte[] signature = Main.getSignature(hash, CLIENT_TEST_USER2);

            PostGeneralRequest requestGeneralPost = PostGeneralRequest.newBuilder()
                    .setPost(post).setSignature(ByteString.copyFrom(signature)).setToken(token).setWts(wts).build();

            PostGeneralResponse responseGeneralPost = stub.postGeneral(requestGeneralPost);
            /*---------------------------------SERVER VALIDATION--------------------------------*/
            ByteString sigServerByteString = responseGeneralPost.getSignature();
            String key = responseGeneralPost.getResult();

            byte[] resultHash = Main.getHashFromObject(key);

            boolean validResponse = validateServerResponse(sigServerByteString, resultHash, responseGeneralPost.getKey());
            if (!validResponse) {
                System.err.println("Invalid signature and/or hash. Post General response corrupted.");
                return null;

            }

            return responseGeneralPost;
        }
    }


}
