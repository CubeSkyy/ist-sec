package com.dpas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

@RunWith(JUnit4.class)
public class RegisterTest {
    private static final String TEST_KEY = "Test Key";
    private static final String TEST_USERNAME = "Test Username";

    private static final String TEST_KEY2 = "Test Key2";
    private static final String TEST_USERNAME2 = "Test Username2";

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @Test
    public void register_once() throws Exception {
        // Generate a unique in-process server name.
        String serverName = InProcessServerBuilder.generateName();

        // Create a server, add service, start, and register for automatic graceful shutdown.
        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName).directExecutor().addService(new HelloWorldServiceImpl()).build().start());

        HelloWorldServiceGrpc.HelloWorldServiceBlockingStub blockingStub = HelloWorldServiceGrpc.newBlockingStub(
                // Create a client channel and register for automatic graceful shutdown.
                grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));


        RegisterResponse reply =
                blockingStub.register(RegisterRequest.newBuilder().setKey(TEST_KEY).setUsername(TEST_USERNAME).build());

        assertTrue(reply.getResult());

    }

    @Test
    public void register_twice_equal() throws Exception {
        // Generate a unique in-process server name.
        String serverName = InProcessServerBuilder.generateName();

        // Create a server, add service, start, and register for automatic graceful shutdown.
        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName).directExecutor().addService(new HelloWorldServiceImpl()).build().start());

        HelloWorldServiceGrpc.HelloWorldServiceBlockingStub blockingStub = HelloWorldServiceGrpc.newBlockingStub(
                // Create a client channel and register for automatic graceful shutdown.
                grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));


        RegisterResponse reply =
                blockingStub.register(RegisterRequest.newBuilder().setKey(TEST_KEY).setUsername(TEST_USERNAME).build());
        RegisterResponse reply2 =
                blockingStub.register(RegisterRequest.newBuilder().setKey(TEST_KEY).setUsername(TEST_USERNAME).build());


        assertTrue(reply.getResult());
        assertTrue(reply2.getResult());

    }

    @Test
    public void register_twice_dif() throws Exception {
        // Generate a unique in-process server name.
        String serverName = InProcessServerBuilder.generateName();

        // Create a server, add service, start, and register for automatic graceful shutdown.
        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName).directExecutor().addService(new HelloWorldServiceImpl()).build().start());

        HelloWorldServiceGrpc.HelloWorldServiceBlockingStub blockingStub = HelloWorldServiceGrpc.newBlockingStub(
                // Create a client channel and register for automatic graceful shutdown.
                grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));


        RegisterResponse reply =
                blockingStub.register(RegisterRequest.newBuilder().setKey(TEST_KEY).setUsername(TEST_USERNAME).build());
        RegisterResponse reply2 =
                blockingStub.register(RegisterRequest.newBuilder().setKey(TEST_KEY2).setUsername(TEST_USERNAME2).build());


        assertTrue(reply.getResult());
        assertTrue(reply2.getResult());

    }

    @Test
    public void register_empty_key() throws Exception {
        // Generate a unique in-process server name.
        String serverName = InProcessServerBuilder.generateName();

        // Create a server, add service, start, and register for automatic graceful shutdown.
        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName).directExecutor().addService(new HelloWorldServiceImpl()).build().start());

        HelloWorldServiceGrpc.HelloWorldServiceBlockingStub blockingStub = HelloWorldServiceGrpc.newBlockingStub(
                // Create a client channel and register for automatic graceful shutdown.
                grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));


        RegisterResponse reply =
                blockingStub.register(RegisterRequest.newBuilder().setKey("").setUsername(TEST_USERNAME).build());

        assertTrue(reply.getResult());

    }

    @Test
    public void register_empty_username() throws Exception {
        // Generate a unique in-process server name.
        String serverName = InProcessServerBuilder.generateName();

        // Create a server, add service, start, and register for automatic graceful shutdown.
        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName).directExecutor().addService(new HelloWorldServiceImpl()).build().start());

        HelloWorldServiceGrpc.HelloWorldServiceBlockingStub blockingStub = HelloWorldServiceGrpc.newBlockingStub(
                // Create a client channel and register for automatic graceful shutdown.
                grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));


        RegisterResponse reply =
                blockingStub.register(RegisterRequest.newBuilder().setKey(TEST_KEY).setUsername("").build());

        assertTrue(reply.getResult());

    }
}