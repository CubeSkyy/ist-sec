package com.dpas;


import com.dpas.client.ClientAPI;
import com.dpas.crypto.Main;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.dpas.ClientDataStore.*;
import static com.dpas.server.ServerDataStore.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class ByzantineClientTest extends RollbackTestAbstractClass {

    @Test
    public void changeMessageBeforeBCB() throws Exception {
        changeMessageBCBAPI client = new changeMessageBCBAPI(numOfServers, numOfFaults);

        client.receive(stubs, "register|" + CLIENT_TEST_USER);
        ArrayList<Dpas.PostResponse> res = (ArrayList) client.receive(stubs, "post|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG);
        System.out.println(errContent.toString());

        assertTrue(StringUtils.countMatches(errContent.toString(), MSG_ERROR_BCB) >= client.majority);
        assertEquals(0, res.size());

    }

    @Test
    public void changeMessagePostAPI() throws Exception {
        changeMessagePostAPI client = new changeMessagePostAPI(numOfServers, numOfFaults);

        client.receive(stubs, "register|" + CLIENT_TEST_USER);

        ArrayList<Dpas.PostResponse> res = (ArrayList) client.receive(stubs, "post|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG);

        assertTrue(StringUtils.countMatches(errContent.toString(), MSG_ERROR_BCB) >= client.majority);
        assertEquals(0, res.size());

    }

    private class changeMessageBCBAPI extends ClientAPI {
        public changeMessageBCBAPI(int numOfServers, int numOfFaults) {
            super(numOfServers, numOfFaults);
        }

        int counter = 1;

        @Override
        public Dpas.Announcement buildAnnouncement(String[] command) {
            String userAlias = command[1];
            List<Integer> referral = new ArrayList<Integer>();
            if (command.length > 2) {
                int i = 3;
                while (i < command.length) {
                    referral.add(Integer.parseInt(command[i]));
                    i += 1;
                }
            }
            String message;
            if (counter % 2 == 1)
                message = CLIENT_TEST_MSG2;
            else
                message = command[2];
            counter++;
            Dpas.Announcement.Builder post = Dpas.Announcement.newBuilder().setKey(userAlias).setMessage(message).setWts(wts).setGeneral(command[0].equals("postGeneral"));
            post.addAllRef(referral);
            return post.build();
        }
    }

    private class changeMessagePostAPI extends ClientAPI {
        public changeMessagePostAPI(int numOfServers, int numOfFaults) {
            super(numOfServers, numOfFaults);
        }

        int counter = 1;

        @Override
        public Dpas.PostResponse post(DpasServiceGrpc.DpasServiceBlockingStub stub, Object payload, ArrayList<GeneratedMessageV3> bcb) throws Exception {
            String[] command = (String[]) payload;

            String userAlias = command[1];
            /*-------------------------------GET TOKEN VALIDATION-------------------------------*/
            Dpas.GetTokenResponse responseGetToken = getClientToken(stub, userAlias);

            boolean tokenVerify = validateToken(responseGetToken);

            if (!tokenVerify) {
                System.err.println("Invalid signature and/or hash. GetToken request corrupted.");
                return null;
            }
            /*----------------------------------------------------------------------------------*/
            String token = responseGetToken.getToken();

            Dpas.Announcement post = (Dpas.Announcement) bcb.get(0).getField(bcb.get(0).getDescriptorForType().findFieldByName("post"));

            Object[] obj_list = {post.getKey(), post.getMessage(), token, wts};
            byte[] signature = Main.getSignatureAll(obj_list, userAlias);
            Object[] obj_list2 = {post.getKey(), post.getMessage(), post.getRefList()};
            byte[] announcementSig = Main.getSignatureAll(obj_list2, userAlias);

            ArrayList<Dpas.BroadcastResponse> bcbCast = (ArrayList<Dpas.BroadcastResponse>) bcb.stream().map(obj -> (Dpas.BroadcastResponse) obj).collect(Collectors.toList());
            if(counter % 2 == 1){
                Dpas.Announcement.Builder postBuilder = post.toBuilder();
                postBuilder.setMessage(CLIENT_TEST_MSG2);
                post = postBuilder.build();
            }

            Dpas.PostRequest requestPost = Dpas.PostRequest.newBuilder().setPost(post).setAnnouncementSig(ByteString.copyFrom(announcementSig)).setSignature(ByteString.copyFrom(signature))
                    .setWts(wts).setToken(token).addAllBcb(bcbCast).build();

            Dpas.PostResponse responsePost = stub.post(requestPost);

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
    }
}




