package com.dpas;

import com.dpas.client.ClientAPI;
import com.dpas.server.DpasServiceImpl;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.util.ArrayList;


public abstract class RollbackTestAbstractClass {

    static public DpasServiceGrpc.DpasServiceBlockingStub blockingStub;
    public ManagedChannel inProcessChannel;
    static String host = "Localhost";
    static public ClientAPI client;
    static int initialPort = 8080;
    static int numOfFaults = 1;
    static int numOfServers = 3 * numOfFaults + 1;
    static String target;
    static ArrayList<Server> servers = new ArrayList<Server>();

    static ArrayList<ManagedChannel> channels = new ArrayList<ManagedChannel>();
    static ArrayList<DpasServiceGrpc.DpasServiceBlockingStub> stubs = new ArrayList<DpasServiceGrpc.DpasServiceBlockingStub>();
    static ByteArrayOutputStream errContent;



    @After
    @Before
    public void start() {
        errContent = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errContent));
        try {
            client.receive(stubs, "reset");
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @BeforeClass
    public static void setUp() throws Exception {

        // Create a server, add service, start, and register for automatic graceful shutdown.


        int port;
        for (int i = 0; i < numOfServers; i++) {

            port = initialPort + i;
            target = host + ":" + port;


            channels.add(ManagedChannelBuilder.forTarget(target).usePlaintext().build());
            blockingStub = DpasServiceGrpc.newBlockingStub(channels.get(channels.size()-1));
            stubs.add(blockingStub);

            System.out.println("Connected to " + target);
        }


        client = new ClientAPI(numOfServers, numOfFaults);

    }

    @AfterClass
    public static void tearDown() {
        try {
            client.receive(stubs, "reset");
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }

}