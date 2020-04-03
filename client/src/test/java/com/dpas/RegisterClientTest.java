package com.dpas;

import com.dpas.Dpas.RegisterRequest;
import com.dpas.Dpas.RegisterResponse;
import com.dpas.client.ClientAPI;
import com.dpas.crypto.Main;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;

@RunWith(JUnit4.class)
public class RegisterClientTest extends RollbackTestAbstractClass {

    @Test
    public void registerUserTest() throws Exception {
        RegisterResponse response = client.register(blockingStub, client.getCommand("register|" + ClientDataStore.CLIENT_TEST_USER));
        assertNotNull(response.getResult());
    }

    @Test
    public void registerNonUserTest() throws Exception {
        Throwable exception = assertThrows(IOException.class, () -> {
            RegisterResponse response = client.register(blockingStub, client.getCommand("register|" + ClientDataStore.CLIENT_WRONG_USER));
        });
        assertEquals("Private key from that user is not in keystore.", exception.getMessage());
    }

    @Test
    public void registerChangeUserTest() throws Exception {
        RegisterTestWrongUserAPI client = new RegisterTestWrongUserAPI();
        final RegisterResponse[] response = new RegisterResponse[1];
        Throwable exception = assertThrows(io.grpc.StatusRuntimeException.class, () -> {
            response[0] = client.register(blockingStub, client.getCommand("register|" + ClientDataStore.CLIENT_TEST_USER));
        });
        assertEquals("INVALID_ARGUMENT: Invalid signature and/or hash. Register request denied.", exception.getMessage());
        assertNull(response[0]);
    }

    @Test
    public void registerChangeSignatureTest() throws Exception {
        RegisterTestWrongSigAPI client = new RegisterTestWrongSigAPI();
        final RegisterResponse[] response = new RegisterResponse[1];
        Throwable exception = assertThrows(io.grpc.StatusRuntimeException.class, () -> {
            response[0] = client.register(blockingStub, client.getCommand("register|" + ClientDataStore.CLIENT_TEST_USER));
        });
        assertEquals("INVALID_ARGUMENT: Invalid signature and/or hash. Register request denied.", exception.getMessage());
        assertNull(response[0]);
    }


    @Rule
    public final GrpcCleanupRule grpcCleanupTest = new GrpcCleanupRule();

    //TODO: Complete when server has new test user
//    @Test
//    public void registerChangeServerSignatureTest() throws Exception {
//
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
//    }


    private class RegisterTestWrongUserAPI extends ClientAPI {
        @Override
        public RegisterResponse register(DpasServiceGrpc.DpasServiceBlockingStub stub, String[] command) throws Exception {
            String userAlias = command[1];

            byte[] hash = Main.getHashFromObject(command[1]);
            byte[] signature = Main.getSignature(hash, command[1]);

            RegisterRequest requestRegister = RegisterRequest.newBuilder().setKey(ClientDataStore.CLIENT_TEST_USER2)
                    .setSignature(ByteString.copyFrom(signature)).build();

            RegisterResponse responseRegister = stub.register(requestRegister);

            /*---------------------------------SERVER VALIDATION--------------------------------*/
            ByteString sigServerByteString = responseRegister.getSignature();
            String key = responseRegister.getResult();

            byte[] resultHash = Main.getHashFromObject(key);

            boolean validResponse = validateServerResponse(sigServerByteString, resultHash);
            if (!validResponse) {
                System.err.println("Invalid signature and/or hash. Register response corrupted.");
                return null;
            }

            System.out.println("REGISTER: " + responseRegister);
            return responseRegister;
        }
    }


    private class RegisterTestWrongSigAPI extends ClientAPI {
        @Override
        public RegisterResponse register(DpasServiceGrpc.DpasServiceBlockingStub stub, String[] command) throws Exception {
            String userAlias = command[1];

            byte[] hash = Main.getHashFromObject(userAlias);
            byte[] signature = Main.getSignature(hash, ClientDataStore.CLIENT_TEST_USER2);

            RegisterRequest requestRegister = RegisterRequest.newBuilder().setKey(userAlias)
                    .setSignature(ByteString.copyFrom(signature)).build();

            RegisterResponse responseRegister = stub.register(requestRegister);

            /*---------------------------------SERVER VALIDATION--------------------------------*/
            ByteString sigServerByteString = responseRegister.getSignature();
            String key = responseRegister.getResult();

            byte[] resultHash = Main.getHashFromObject(key);

            boolean validResponse = validateServerResponse(sigServerByteString, resultHash);
            if (!validResponse) {
                System.err.println("Invalid signature and/or hash. Register response corrupted.");
                return null;
            }

            System.out.println("REGISTER: " + responseRegister);
            return responseRegister;
        }
    }


    private final DpasServiceGrpc.DpasServiceImplBase serviceImplTest =
            mock(DpasServiceGrpc.DpasServiceImplBase.class, delegatesTo(
                    new DpasServiceGrpc.DpasServiceImplBase() {

                        @Override
                        public synchronized void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
                            System.out.println("Register Request Received: " + request);
                            String key = request.getKey();
                            /*--------------------------SIGNATURE AND HASH VALIDATE-----------------------------*/
                            ByteString sigByteString = request.getSignature();
                            try {
                                byte[] userHash = Main.getHashFromObject(ClientDataStore.CLIENT_WRONG_USER);
                                byte[] sigGeneral = Main.getSignature(userHash, ClientDataStore.CLIENT_WRONG_USER); //TODO change to test1 when server has new user

                                ByteString responseSigByteString = ByteString.copyFrom(sigGeneral);

                                RegisterResponse response = RegisterResponse.newBuilder()
                                        .setResult(key).setSignature(responseSigByteString).build();

                                responseObserver.onNext(response);
                                responseObserver.onCompleted();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }));

}
