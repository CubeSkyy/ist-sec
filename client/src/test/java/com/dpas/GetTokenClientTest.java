package com.dpas;

import com.dpas.Dpas.GetTokenRequest;
import com.dpas.Dpas.GetTokenResponse;
import com.dpas.client.ClientAPI;
import com.dpas.crypto.Main;
import com.dpas.server.ServerDataStore;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;
import io.grpc.StatusRuntimeException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class GetTokenClientTest extends RollbackTestAbstractClass {

    @Test
    public void getTokenValid() throws Exception {
        Dpas.RegisterResponse registerResponse = client.register(blockingStub, client.getCommand("register|" + ClientDataStore.CLIENT_TEST_USER));
        GetTokenResponse tokenResponse = client.getClientToken(blockingStub, ClientDataStore.CLIENT_TEST_USER);

        assertNotNull(registerResponse.getResult());
        assertTrue(client.validateToken(tokenResponse));
    }

    @Test
    public void getTokenNonRegistedValid() throws Exception {
        final GetTokenResponse[] tokenResponse = new GetTokenResponse[1];
        Throwable exception = assertThrows(io.grpc.StatusRuntimeException.class, () -> {
            tokenResponse[0] = client.getClientToken(blockingStub, ClientDataStore.CLIENT_TEST_USER);
        });
        assertEquals(ServerDataStore.MSG_ERROR_NOT_REGISTERED, ((StatusRuntimeException) exception).getStatus().getDescription());
    }

    @Test
    public void getTokenNonUserTest() throws Exception {
        final GetTokenResponse[] tokenResponse = new GetTokenResponse[1];
        Throwable exception = assertThrows(IOException.class, () -> {
            tokenResponse[0] = client.getClientToken(blockingStub, ClientDataStore.CLIENT_WRONG_USER);
        });
        assertEquals("Private key from that user is not in keystore.", exception.getMessage());
    }

    @Test
    public void getTokenChangeUserTest() throws Exception {
        changeUserAPI client = new changeUserAPI(numOfServers, numOfFaults);
        final GetTokenResponse[] response = new GetTokenResponse[1];
        Throwable exception = assertThrows(io.grpc.StatusRuntimeException.class, () -> {
            response[0] = client.getClientToken(blockingStub, ClientDataStore.CLIENT_TEST_USER);
        });
        assertEquals(ServerDataStore.MSG_ERROR_GETTOKEN_SIG, ((StatusRuntimeException) exception).getStatus().getDescription());
        assertNull(response[0]);
    }

    @Test
    public void getTokenChangeSignatureTest() throws Exception {
        changeSignatureAPI client = new changeSignatureAPI(numOfServers, numOfFaults);
        final GetTokenResponse[] response = new GetTokenResponse[1];
        Throwable exception = assertThrows(io.grpc.StatusRuntimeException.class, () -> {
            response[0] = client.getClientToken(blockingStub, ClientDataStore.CLIENT_TEST_USER);
        });

        assertEquals(ServerDataStore.MSG_ERROR_GETTOKEN_SIG, ((StatusRuntimeException) exception).getStatus().getDescription());
        assertNull(response[0]);
    }

    @Test
    public void getTokenChangeResponseTokenTest() throws Exception {
        changeResponseTokenTestAPI client = new changeResponseTokenTestAPI(numOfServers, numOfFaults);

        Dpas.RegisterResponse registerResponse = client.register(blockingStub, client.getCommand("register|" + ClientDataStore.CLIENT_TEST_USER));
        GetTokenResponse tokenResponse = client.getClientToken(blockingStub, ClientDataStore.CLIENT_TEST_USER);

        assertNotNull(registerResponse.getResult());
        assertFalse(client.validateToken(tokenResponse));
    }

    //TODO: Complete when server has new test user
    @Test
    public void getTokenChangeServerSignatureTest() throws Exception {
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


    private class changeUserAPI extends ClientAPI {
        public changeUserAPI(int numOfServers, int numOfFaults) {
            super(numOfServers, numOfFaults);
        }

        @Override
        public GetTokenResponse getClientToken(DpasServiceGrpc.DpasServiceBlockingStub stub, String userAlias) throws Exception {
            byte[] keyHash = Main.getHashFromObject(userAlias);
            byte[] keySig = Main.getSignature(keyHash, userAlias);

            GetTokenRequest requestGetToken = GetTokenRequest.newBuilder().setKey(ClientDataStore.CLIENT_TEST_USER2)
                    .setSignature(ByteString.copyFrom(keySig)).build();
            GetTokenResponse responseGetToken = stub.getToken(requestGetToken);
            System.out.println("GET TOKEN: " + responseGetToken);
            return responseGetToken;
        }
    }


    private class changeSignatureAPI extends ClientAPI {
        public changeSignatureAPI(int numOfServers, int numOfFaults) {
            super(numOfServers, numOfFaults);
        }

        @Override
        public GetTokenResponse getClientToken(DpasServiceGrpc.DpasServiceBlockingStub stub, String userAlias) throws Exception {
            byte[] keyHash = Main.getHashFromObject(userAlias);
            byte[] keySig = Main.getSignature(keyHash, ClientDataStore.CLIENT_TEST_USER2);

            GetTokenRequest requestGetToken = GetTokenRequest.newBuilder().setKey(userAlias)
                    .setSignature(ByteString.copyFrom(keySig)).build();
            GetTokenResponse responseGetToken = stub.getToken(requestGetToken);
            System.out.println("GET TOKEN: " + responseGetToken);
            return responseGetToken;
        }
    }

    private class changeResponseTokenTestAPI extends ClientAPI {
        public changeResponseTokenTestAPI(int numOfServers, int numOfFaults) {
            super(numOfServers, numOfFaults);
        }

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

}
