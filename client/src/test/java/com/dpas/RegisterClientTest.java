package com.dpas;

import com.dpas.Dpas.RegisterRequest;
import com.dpas.Dpas.RegisterResponse;
import com.dpas.client.ClientAPI;
import com.dpas.crypto.Main;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static com.dpas.server.ServerDataStore.*;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static com.dpas.ClientDataStore.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)

public class RegisterClientTest extends RollbackTestAbstractClass {

    @Test
    public void registerUserTest() throws Exception {

        ArrayList<RegisterResponse> res = (ArrayList) client.receive(stubs, "register|" + CLIENT_TEST_USER);
        for (RegisterResponse ele : res) {
            assertEquals(ele.getResult(), CLIENT_TEST_USER);
        }
    }

    @Test
    public void registerNonUserTest() throws Exception {

        ArrayList<RegisterResponse> res = (ArrayList) client.receive(stubs, "register|" + CLIENT_WRONG_USER);
        assertTrue(StringUtils.countMatches(errContent.toString(), "java.io.IOException: Private key from that user is not in keystore.") >= client.majority);
        assertEquals(0, res.size());
    }

    @Test
    public void registerChangeUserTest() throws Exception {

        RegisterTestWrongUserAPI client = new RegisterTestWrongUserAPI(numOfServers, numOfFaults);

        ArrayList<RegisterResponse> res = (ArrayList) client.receive(stubs, "register|" + CLIENT_TEST_USER);

        assertTrue(StringUtils.countMatches(errContent.toString(), MSG_ERROR_BCB) >= client.majority);
        assertEquals(0, res.size());

    }

    @Test
    public void registerChangeSignatureTest() throws Exception {
        RegisterTestWrongSigAPI client = new RegisterTestWrongSigAPI(numOfServers, numOfFaults);

        ArrayList<RegisterResponse> res = (ArrayList) client.receive(stubs, "register|" + CLIENT_TEST_USER);

        assertTrue(StringUtils.countMatches(errContent.toString(), MSG_ERROR_BCB) >= client.majority);
        assertEquals(0, res.size());

    }

    private class RegisterTestWrongUserAPI extends ClientAPI {
        public RegisterTestWrongUserAPI(int numOfServers, int numOfFaults) {
            super(numOfServers, numOfFaults);
        }
        public RegisterResponse register(DpasServiceGrpc.DpasServiceBlockingStub stub, Object payload, ArrayList<GeneratedMessageV3> bcb) throws Exception {
            String[] command = (String[]) payload;

            String userAlias = command[1];
            Object[] obj_list = {userAlias};

            byte[] signature = Main.getSignatureAll(obj_list, userAlias);
            ArrayList<Dpas.BroadcastRegisterResponse> bcbCast = (ArrayList<Dpas.BroadcastRegisterResponse>) bcb.stream().map(obj -> (Dpas.BroadcastRegisterResponse) obj).collect(Collectors.toList());

            RegisterRequest requestRegister = RegisterRequest.newBuilder().setKey(CLIENT_TEST_USER2)
                    .setSignature(ByteString.copyFrom(signature)).addAllBcb(bcbCast).build();

            RegisterResponse responseRegister = stub.register(requestRegister);

            /*---------------------------------SERVER VALIDATION--------------------------------*/
            ByteString sigServerByteString = responseRegister.getSignature();
            String key = responseRegister.getResult();
            String serverAlias = responseRegister.getKey();
            Object[] obj_list2 = {key, serverAlias};
            boolean validResponse = validateServerResponse(sigServerByteString, obj_list2, responseRegister.getKey());
            if (!validResponse) {
                return null;
            }

            return responseRegister;
        }

    }


    private class RegisterTestWrongSigAPI extends ClientAPI {
        public RegisterTestWrongSigAPI(int numOfServers, int numOfFaults) {
            super(numOfServers, numOfFaults);
        }

        @Override
        public RegisterResponse register(DpasServiceGrpc.DpasServiceBlockingStub stub, Object payload, ArrayList<GeneratedMessageV3> bcb) throws Exception {
            String[] command = (String[]) payload;

            String userAlias = command[1];
            Object[] obj_list = {CLIENT_TEST_USER2};

            byte[] signature = Main.getSignatureAll(obj_list, userAlias);
            ArrayList<Dpas.BroadcastRegisterResponse> bcbCast = (ArrayList<Dpas.BroadcastRegisterResponse>) bcb.stream().map(obj -> (Dpas.BroadcastRegisterResponse) obj).collect(Collectors.toList());

            RegisterRequest requestRegister = RegisterRequest.newBuilder().setKey(CLIENT_TEST_USER2)
                    .setSignature(ByteString.copyFrom(signature)).addAllBcb(bcbCast).build();

            RegisterResponse responseRegister = stub.register(requestRegister);

            /*---------------------------------SERVER VALIDATION--------------------------------*/
            ByteString sigServerByteString = responseRegister.getSignature();
            String key = responseRegister.getResult();
            String serverAlias = responseRegister.getKey();
            Object[] obj_list2 = {key, serverAlias};
            boolean validResponse = validateServerResponse(sigServerByteString, obj_list2, responseRegister.getKey());
            if (!validResponse) {
                return null;
            }

            return responseRegister;
        }

    }


}
