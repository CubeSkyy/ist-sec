package com.dpas;

import com.dpas.Dpas.ResetRequest;
import com.dpas.client.ClientAPI;
import com.dpas.server.DpasServiceImpl;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;

import static com.dpas.server.ServerDataStore.*;



public abstract class RollbackTestAbstractClass {

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();
    public DpasServiceGrpc.DpasServiceBlockingStub blockingStub;
    public ManagedChannel inProcessChannel;
    String host = "Localhost";
    static public ClientAPI client;
    static int initialPort = 8080;
    static int numOfFaults = 1;
    static int numOfServers = 3 * numOfFaults + 1;
    static String target;
    static ArrayList<ManagedChannel> channels = new ArrayList<ManagedChannel>();
    static ArrayList<DpasServiceGrpc.DpasServiceBlockingStub> stubs = new ArrayList<DpasServiceGrpc.DpasServiceBlockingStub>();
    static ByteArrayOutputStream errContent = new ByteArrayOutputStream();;

    @AfterClass
    @BeforeClass
    public static void start() {
        System.setErr( new PrintStream( errContent ) );
        try {
            client.receive(stubs, "reset");
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @Before
    public void setUp() throws Exception {

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

    @After
    public void tearDown() {
        client.wts = 0;
        try {
            client.receive(stubs, "reset");
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }

}