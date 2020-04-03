package com.dpas;

import com.dpas.client.ClientAPI;
import com.dpas.server.HelloWorldServiceImpl;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.*;

import java.io.File;

import static com.dpas.server.ServerDataStore.*;


public abstract class RollbackTestAbstractClass {
    public HelloWorldServiceGrpc.HelloWorldServiceBlockingStub blockingStub;
    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();
    public ManagedChannel inProcessChannel;

    public ClientAPI client;

    @Before
    public void setUp() throws Exception {
        // Generate a unique in-process server name.
        String serverName = InProcessServerBuilder.generateName();

        // Create a server, add service, start, and register for automatic graceful shutdown.
        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName).directExecutor().addService(HelloWorldServiceImpl.getInstance()).build().start());


        inProcessChannel = grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build());

        blockingStub = HelloWorldServiceGrpc.newBlockingStub(inProcessChannel);

        client = ClientAPI.getInstance();

    }

    @AfterClass
    @BeforeClass
    public static void  start() {
        new File(USERS_FILE).delete();
        new File(PARTICULAR_FILE).delete();
        new File(GENERAL_FILE).delete();
        new File(POSTID_FILE).delete();
    }

    @After
    public void tearDown() {
        new File(USERS_FILE).delete();
        new File(PARTICULAR_FILE).delete();
        new File(GENERAL_FILE).delete();
        new File(POSTID_FILE).delete();
        blockingStub.reset(HelloWorld.ResetRequest.newBuilder().build());
    }

}