package com.dpas;

import static org.junit.Assert.assertEquals;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.dpas.client.HelloClient;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

@RunWith(JUnit4.class)
public class HelloWorldClientTest {
    /**
     * This rule manages automatic graceful shutdown for the registered servers and channels at the
     * end of test.
     */
    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    private final HelloWorldServiceGrpc.HelloWorldServiceImplBase serviceImpl =
            mock(HelloWorldServiceGrpc.HelloWorldServiceImplBase.class, delegatesTo(
                    new HelloWorldServiceGrpc.HelloWorldServiceImplBase() {

                         @Override
                         public void greeting(HelloWorld.HelloRequest request, StreamObserver<HelloWorld.HelloResponse> respObserver) {
                           respObserver.onNext(HelloWorld.HelloResponse.getDefaultInstance());
                           respObserver.onCompleted();
                         }
                    }));

    private HelloClient client;

    @Before
    public void setUp() throws Exception {
        // Generate a unique in-process server name.
        String serverName = InProcessServerBuilder.generateName();

        // Create a server, add service, start, and register for automatic graceful shutdown.
        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName).directExecutor().addService(serviceImpl).build().start());

        // Create a client channel and register for automatic graceful shutdown.
        ManagedChannel channel = grpcCleanup.register(
                InProcessChannelBuilder.forName(serverName).directExecutor().build());

        // Create a HelloWorldClient using the in-process channel;
        client = new HelloClient(channel);
    }

    /**
     * To test the client, call from the client against the fake server, and verify behaviors or state
     * changes from the server side.
     */
    @Test
    public void greet_messageDeliveredToServer() {
        ArgumentCaptor<HelloWorld.HelloRequest> requestCaptor = ArgumentCaptor.forClass(HelloWorld.HelloRequest.class);

        client.greet("test name");

        verify(serviceImpl)
                .greeting(requestCaptor.capture(), ArgumentMatchers.any());
        assertEquals("test name", requestCaptor.getValue().getName());
    }
}