package com.dpas.server;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

public class DpasServer {

    public static void main(String[] args) throws Exception {
        // check arguments
        if (args.length < 1) {
            System.err.println("Argument(s) missing!");
            System.err.printf("Usage: java %s port%n", Server.class.getName());
            return;
        }

        final int port = Integer.parseInt(args[0]);
        final BindableService impl = DpasServiceImpl.getInstance();

        // Create a new server to listen on port
        Server server = ServerBuilder.forPort(port).addService(impl).build();

        // Start the server
        server.start();
        // Server threads are running in the background.
        System.out.println("Server started");

        // Do not exit the main thread. Wait until server is terminated.
        server.awaitTermination();
    }

}
