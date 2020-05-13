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

import static com.dpas.ClientDataStore.*;
import static org.junit.Assert.assertTrue;


@RunWith(JUnit4.class)
public class ByzantineServerTest extends RollbackTestAbstractClass {

    @Test
    public void readAfterChange() throws Exception {
        changeMessageReceived client = new changeMessageReceived(numOfServers, numOfFaults);
        client.receive(stubs, "register|" + CLIENT_TEST_USER);
        client.receive(stubs, "post|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG);
        ArrayList<Dpas.RegisterResponse> res = (ArrayList) client.receive(stubs, "read|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG_NUMBER);

        assertTrue(StringUtils.countMatches(errContent.toString(), "Invalid read signature. Read was corrupted") >= client.majority);
        assertTrue(res.size() == 0);
    }


    private class changeMessageReceived extends ClientAPI {
        public changeMessageReceived(int numOfServers, int numOfFaults) {
            super(numOfServers, numOfFaults);
        }

        @Override
        public Dpas.ReadResponse read(DpasServiceGrpc.DpasServiceBlockingStub stub, Object payload, ArrayList<GeneratedMessageV3> bcb) throws Exception {
            String[] command = (String[]) payload;

            String userAlias = command[1];
            String key = command[2];
            int number = Integer.parseInt(command[3]);
            /*-------------------------------GET TOKEN VALIDATION-------------------------------*/
            Dpas.GetTokenResponse responseGetToken = getClientToken(stub, userAlias);

            boolean tokenVerify = validateToken(responseGetToken);

            if (!tokenVerify) {
                System.err.println("Invalid signature and/or hash. GetToken request corrupted.");
                return null;
            }
            /*----------------------------------------------------------------------------------*/
            String token = responseGetToken.getToken();
            Object[] obj_list = {userAlias, key, number, token};
            byte[] signature = Main.getSignatureAll(obj_list, userAlias);

            Dpas.ReadRequest requestRead = Dpas.ReadRequest.newBuilder().setNumber(number).setKey(userAlias)
                    .setKeyToRead(key).setSignature(ByteString.copyFrom(signature)).setToken(token).build();

            Dpas.ReadResponse responseRead = stub.read(requestRead);

            /*---------------------------------SERVER VALIDATION--------------------------------*/
            ByteString sigServerByteString = responseRead.getSignature();
            ArrayList<Dpas.Announcement> result = new ArrayList<Dpas.Announcement>(responseRead.getResultList());

            String serverAlias = responseRead.getKey();
            Object[] obj_list2 = {result, responseRead.getTsId(), responseRead.getTs(), serverAlias};

            boolean validResponse = validateServerResponse(sigServerByteString, obj_list2, responseRead.getKey());
            if (!validResponse) {
                System.err.println("Invalid signature and/or hash. Read Response corrupted.");
                return null;
            }
            responseRead = responseRead.toBuilder().setResult(0, responseRead.toBuilder().getResult(0).toBuilder().setMessage("Changed Message").build()).build();

            if (verifyWriteSig(responseRead)) return responseRead;
            else {
                String message = "Invalid read signature. Read was corrupted.";
                throw new Exception(message);
            }
        }
    }
}