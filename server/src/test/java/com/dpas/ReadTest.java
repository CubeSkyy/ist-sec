package com.dpas;

import com.dpas.server.HelloWorldServiceImpl;
import com.google.protobuf.ByteString;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import com.dpas.HelloWorld.RegisterRequest;
import com.dpas.HelloWorld.RegisterResponse;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class ReadTest extends RollbackTestAbstractClass {
    private static final String TEST_KEY = "Test Key";
    private static final String TEST_USERNAME = "Test Username";

    private static final String TEST_KEY2 = "Test Key2";
    private static final String TEST_USERNAME2 = "Test Username2";


    @Test
    public void readGeneralNoRef() throws Exception {
        RegisterResponse registerReply =
                blockingStub.register(RegisterRequest.newBuilder().setKey(DataStore.TEST_KEY).setUsername(DataStore.TEST_USERNAME).build());


        HelloWorld.Announcement generalPost = HelloWorld.Announcement.newBuilder().setKey(DataStore.TEST_KEY).setMessage(DataStore.TEST_MESSAGE).build();
        HelloWorld.PostGeneralRequest postRequest = HelloWorld.PostGeneralRequest.newBuilder().setPost(generalPost).setSignature(ByteString.EMPTY).build(); //TODO:Sign post when crypto module is implemented
        HelloWorld.PostGeneralResponse postResponse = blockingStub.postGeneral(postRequest);

        HelloWorld.ReadGeneralRequest readRequest = HelloWorld.ReadGeneralRequest.newBuilder().setNumber(1).build();
        HelloWorld.ReadGeneralResponse readResponse = blockingStub.readGeneral(readRequest);


        assertTrue(registerReply.getResult());
        assertTrue(postResponse.getResult());
        //TODO: Check signature when crypto module is implemented
        assertEquals(readResponse.getResult(0).getMessage(),DataStore.TEST_MESSAGE);
        assertEquals(readResponse.getResult(0).getKey(),DataStore.TEST_KEY);
    }

    @Test
    public void readGeneralWrongRef() throws Exception {
        RegisterResponse registerReply =
                blockingStub.register(RegisterRequest.newBuilder().setKey(DataStore.TEST_KEY).setUsername(DataStore.TEST_USERNAME).build());

        HelloWorld.Announcement refPost = HelloWorld.Announcement.newBuilder().setKey(DataStore.TEST_KEY2).setMessage(DataStore.TEST_MESSAGE).build();

        HelloWorld.Announcement generalPost = HelloWorld.Announcement.newBuilder().setKey(DataStore.TEST_KEY).setMessage(DataStore.TEST_MESSAGE).addA(refPost).build();
        HelloWorld.PostGeneralRequest postRequest = HelloWorld.PostGeneralRequest.newBuilder().setPost(generalPost).setSignature(ByteString.EMPTY).build(); //TODO:Sign post when crypto module is implemented
        HelloWorld.PostGeneralResponse postResponse = blockingStub.postGeneral(postRequest);

        assertTrue(registerReply.getResult());
        assertFalse(postResponse.getResult()); //TODO:Do we need to check if referred posts exist?
    }
}