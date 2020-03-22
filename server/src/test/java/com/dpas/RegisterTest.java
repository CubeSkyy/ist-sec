package com.dpas;

import com.dpas.server.HelloWorldServiceImpl;
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
public class RegisterTest extends RollbackTestAbstractClass{

    @Test
    public void register_once() throws Exception {
        RegisterResponse reply =
                blockingStub.register(RegisterRequest.newBuilder().setKey(DataStore.TEST_KEY).setUsername(DataStore.TEST_USERNAME).build());

        assertTrue(reply.getResult());

    }

    @Test
    public void register_twice_equal() throws Exception {
        RegisterResponse reply =
                blockingStub.register(RegisterRequest.newBuilder().setKey(DataStore.TEST_KEY).setUsername(DataStore.TEST_USERNAME).build());
        RegisterResponse reply2 =
                blockingStub.register(RegisterRequest.newBuilder().setKey(DataStore.TEST_KEY).setUsername(DataStore.TEST_USERNAME).build());


        assertTrue(reply.getResult());
        assertTrue(reply2.getResult());

    }

    @Test
    public void register_twice_dif() throws Exception {
        RegisterResponse reply =
                blockingStub.register(RegisterRequest.newBuilder().setKey(DataStore.TEST_KEY).setUsername(DataStore.TEST_USERNAME).build());
        RegisterResponse reply2 =
                blockingStub.register(RegisterRequest.newBuilder().setKey(DataStore.TEST_KEY2).setUsername(DataStore.TEST_USERNAME2).build());


        assertTrue(reply.getResult());
        assertTrue(reply2.getResult());

    }

    @Test
    public void register_empty_key() throws Exception {
        RegisterResponse reply =
                blockingStub.register(RegisterRequest.newBuilder().setKey("").setUsername(DataStore.TEST_USERNAME).build());

        assertFalse(reply.getResult());

    }

    @Test
    public void register_empty_username() throws Exception {
        RegisterResponse reply =
                blockingStub.register(RegisterRequest.newBuilder().setKey(DataStore.TEST_KEY).setUsername("").build());

        assertFalse(reply.getResult());

    }

    @Test
    public void register_null_username() throws Exception {
        RegisterResponse reply =
                blockingStub.register(RegisterRequest.newBuilder().setKey(DataStore.TEST_KEY).setUsername(null).build());

        assertFalse(reply.getResult());

    }

    @Test
    public void register_null_key() throws Exception {
        RegisterResponse reply =
                blockingStub.register(RegisterRequest.newBuilder().setKey(null).setUsername(DataStore.TEST_USERNAME).build());

        assertFalse(reply.getResult());

    }
}