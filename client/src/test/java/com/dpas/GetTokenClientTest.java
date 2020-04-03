package com.dpas;

import com.dpas.HelloWorld.GetTokenResponse;
import com.dpas.client.ClientAPI;
import com.dpas.crypto.Main;
import com.dpas.server.ServerDataStore;
import com.google.protobuf.ByteString;
import io.grpc.StatusRuntimeException;
import org.junit.Test;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

@RunWith(JUnit4.class)
public class GetTokenClientTest extends RollbackTestAbstractClass {

    @Test
    public void getTokenValid() throws Exception {
        GetTokenResponse tokenResponse = client.getClientToken(blockingStub, ClientDataStore.CLIENT_TEST_USER);

        assertTrue(client.validateToken(tokenResponse));
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
        changeUserAPI client = new changeUserAPI();
        final GetTokenResponse[] response = new GetTokenResponse[1];
        Throwable exception = assertThrows(io.grpc.StatusRuntimeException.class, () -> {
            response[0] = client.getClientToken(blockingStub, ClientDataStore.CLIENT_TEST_USER);
        });
        assertEquals(ServerDataStore.MSG_ERROR_GETTOKEN_SIG, ((StatusRuntimeException) exception).getStatus().getDescription());
        assertNull(response[0]);
    }

    @Test
    public void getTokenChangeSignatureTest() throws Exception {
        changeSignatureAPI client = new changeSignatureAPI();
        final GetTokenResponse[] response = new GetTokenResponse[1];
        Throwable exception = assertThrows(io.grpc.StatusRuntimeException.class, () -> {
            response[0] = client.getClientToken(blockingStub, ClientDataStore.CLIENT_TEST_USER);
        });

        assertEquals(ServerDataStore.MSG_ERROR_GETTOKEN_SIG, ((StatusRuntimeException) exception).getStatus().getDescription());
        assertNull(response[0]);
    }

    @Test
    public void getTokenChangeResponseTokenTest() throws Exception {
        changeResponseTokenTestAPI client = new changeResponseTokenTestAPI();
        GetTokenResponse tokenResponse = client.getClientToken(blockingStub, ClientDataStore.CLIENT_TEST_USER);

        assertFalse(client.validateToken(tokenResponse));
    }

    //TODO: Complete when server has new test user
    @Test
    public void getTokenChangeServerSignatureTest() throws Exception {
//        ManagedChannel inProcessChannelTest;
//        HelloWorldServiceGrpc.HelloWorldServiceBlockingStub blockingStubTest;
//        String serverName = InProcessServerBuilder.generateName();
//
//        grpcCleanupTest.register(InProcessServerBuilder
//                .forName(serverName).directExecutor().addService(serviceImplTest).build().start());
//
//        inProcessChannelTest = grpcCleanupTest.register(
//                InProcessChannelBuilder.forName(serverName).directExecutor().build());
//
//        blockingStubTest = HelloWorldServiceGrpc.newBlockingStub(inProcessChannelTest);
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
        @Override
        public GetTokenResponse getClientToken(HelloWorldServiceGrpc.HelloWorldServiceBlockingStub stub, String userAlias) throws Exception {
            byte[] keyHash = Main.getHashFromObject(userAlias);
            byte[] keySig = Main.getSignature(keyHash, userAlias);

            HelloWorld.GetTokenRequest requestGetToken = HelloWorld.GetTokenRequest.newBuilder().setKey(ClientDataStore.CLIENT_TEST_USER2)
                    .setSignature(ByteString.copyFrom(keySig)).build();
            GetTokenResponse responseGetToken = stub.getToken(requestGetToken);
            System.out.println("GET TOKEN: " + responseGetToken);
            return responseGetToken;
        }
    }


    private class changeSignatureAPI extends ClientAPI {
        @Override
        public GetTokenResponse getClientToken(HelloWorldServiceGrpc.HelloWorldServiceBlockingStub stub, String userAlias) throws Exception {
            byte[] keyHash = Main.getHashFromObject(userAlias);
            byte[] keySig = Main.getSignature(keyHash, ClientDataStore.CLIENT_TEST_USER2);

            HelloWorld.GetTokenRequest requestGetToken = HelloWorld.GetTokenRequest.newBuilder().setKey(userAlias)
                    .setSignature(ByteString.copyFrom(keySig)).build();
            GetTokenResponse responseGetToken = stub.getToken(requestGetToken);
            System.out.println("GET TOKEN: " + responseGetToken);
            return responseGetToken;
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

//    private final HelloWorldServiceGrpc.HelloWorldServiceImplBase serviceImplTest =
//            mock(HelloWorldServiceGrpc.HelloWorldServiceImplBase.class, delegatesTo(
//                    new HelloWorldServiceGrpc.HelloWorldServiceImplBase() {
//                        @Override
//                        public synchronized void getToken(HelloWorld.GetTokenRequest request, StreamObserver<GetTokenResponse> responseObserver) {
//
//                            String token = ClientDataStore.WRONG_TOKEN;
//
//                            try {
//                                byte[] hashServer = Main.getHashFromObject(token);
//                                byte[] sigServer = Main.getSignature(hashServer, "test1"); //TODO change to serverAlias when we have multiple servers
//
//                                ByteString sigServerByteString = ByteString.copyFrom(sigServer);
//
//                                /*----------------------------------------------------------------------------------*/
//
//                                GetTokenResponse response = GetTokenResponse.newBuilder().setToken(token).setSignature(sigServerByteString).build();
//
//                                responseObserver.onNext(response);
//                                responseObserver.onCompleted();
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }));

}
