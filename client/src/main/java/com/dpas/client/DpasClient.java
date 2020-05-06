package com.dpas.client;

/* these are generated by the hello-world-server contract */

import com.dpas.DpasServiceGrpc;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import java.util.ArrayList;
import java.util.Collections;

public class DpasClient {
    private static final Logger logger = Logger.getLogger(DpasClient.class.getName());
    private final DpasServiceGrpc.DpasServiceBlockingStub blockingStub;

    public DpasClient(Channel channel) {
        blockingStub = DpasServiceGrpc.newBlockingStub(channel);
    }

    private static BufferedReader reader;
    public static String DEMO1;
    public static String DEMO2;
    public static String DEMO3;


    public static void main(String[] args) throws Exception {

        if (args.length < 3) {
            System.err.println("Argument(s) missing!");
            System.err.printf("Usage: java %s host port numOfFaults%n", DpasClient.class.getName());
            return;
        }

        final String host = args[0];
        final int initialPort = Integer.parseInt(args[1]);
        final int numOfFaults = Integer.parseInt(args[2]);
        final int numOfServers = 3 * numOfFaults + 1;
        int port;
        String target;
        ArrayList<ManagedChannel> channels = new ArrayList<ManagedChannel>();
        ArrayList<DpasServiceGrpc.DpasServiceBlockingStub> stubs = new ArrayList<DpasServiceGrpc.DpasServiceBlockingStub>();

        for (int i = 0; i < numOfServers; i++) {
            port = initialPort + i;
            target = host + ":" + port;
            System.out.println("Connected to " + target);
            channels.add(ManagedChannelBuilder.forTarget(target).usePlaintext().build());
            stubs.add(DpasServiceGrpc.newBlockingStub(channels.get(channels.size()-1)));
        }

        ClientAPI library = ClientAPI.getInstance();
        library.majority = (int) Math.ceil((numOfServers + numOfFaults) / 2.0);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    Thread.sleep(200);
                    System.out.println("Shutting down client...");
                    for (ManagedChannel channel : channels) {
                        channel.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }
        });

        DEMO1 = "register|user1\npost|user1|Test\nread|user1|user1|0";
        DEMO2 = "register|user1\nregister|user2\npostGeneral|user1|Test\npostGeneral|user2|Test2|1\nreadGeneral|user1|0";
        DEMO3 = "register|user1\npost|user1|Test\npost|user1|Test2\npost|user1|Test3\nread|user1|user1|2";

        System.out.print("Insert commands:\n");
        while (true) {

            try {
                System.out.println("");
                reader = new BufferedReader(new InputStreamReader(System.in));
                String input = reader.readLine();
                if(input.equals("exit")){
                 return;
                }
                String[] commands = input.split("\n");
                for (String command : commands) {
                    library.receive(stubs, command);
                }
            } catch (Exception e) {
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
            }
        }


    }

}
