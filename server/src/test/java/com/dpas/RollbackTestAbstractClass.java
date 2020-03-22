package com.dpas;

import com.dpas.server.HelloWorldServiceImpl;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.After;
import org.junit.Before;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Rule;
import java.io.File;

import static com.dpas.server.HelloWorldServiceImpl.*;


public abstract class RollbackTestAbstractClass {
    public HelloWorldServiceGrpc.HelloWorldServiceBlockingStub blockingStub;
    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @Before
    public void setUp() throws Exception {
        // Generate a unique in-process server name.
        String serverName = InProcessServerBuilder.generateName();

        // Create a server, add service, start, and register for automatic graceful shutdown.
        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName).directExecutor().addService(new HelloWorldServiceImpl()).build().start());

        blockingStub = HelloWorldServiceGrpc.newBlockingStub(
                // Create a client channel and register for automatic graceful shutdown.
                grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));

    }

    @After
    public void tearDown() {
        new File(USERS_FILE).delete();
        new File(PARTICULAR_FILE).delete();
        new File(GENERAL_FILE).delete();
    }

}