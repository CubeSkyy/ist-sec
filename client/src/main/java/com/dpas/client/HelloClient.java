package com.dpas.client;

/* these are generated by the hello-world-server contract */
import com.dpas.HelloWorld;
import com.dpas.HelloWorldServiceGrpc;
import java.util.logging.Logger;
import io.grpc.StatusRuntimeException;
import java.util.logging.Level;
import io.grpc.ManagedChannel;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;

public class HelloClient {
	private static final Logger logger = Logger.getLogger(HelloClient.class.getName());
	private final HelloWorldServiceGrpc.HelloWorldServiceBlockingStub blockingStub;

	public HelloClient(Channel channel) {
		// 'channel' here is a Channel, not a ManagedChannel, so it is not this code's responsibility to
		// shut it down.

		// Passing Channels to code makes code easier to test and makes it easier to reuse Channels.
		blockingStub = HelloWorldServiceGrpc.newBlockingStub(channel);
	}

	/** Say hello to server. */
	public void greet(String name) {
		logger.info("Will try to greet " + name + " ...");
		HelloWorld.HelloRequest request = HelloWorld.HelloRequest.newBuilder().setName(name).build();
		HelloWorld.HelloResponse response;
		try {
			response = blockingStub.greeting(request);
		} catch (StatusRuntimeException e) {
			logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
			return;
		}
		logger.info("Greeting: " + response.getGreeting());
	}

	public static void main(String[] args) throws Exception {
		// check arguments
		if (args.length < 2) {
			System.err.println("Argument(s) missing!");
			System.err.printf("Usage: java %s host port%n", HelloClient.class.getName());
			return;
		}

		final String host = args[0];
		final int port = Integer.parseInt(args[1]);
		final String target = host + ":" + port;

		// Channel is the abstraction to connect to a service endpoint
		// Let us use plaintext communication because we do not have certificates
		final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

		// It is up to the client to determine whether to block the call
		// Here we create a blocking stub, but an async stub,
		// or an async stub with Future are always possible.
		HelloWorldServiceGrpc.HelloWorldServiceBlockingStub stub = HelloWorldServiceGrpc.newBlockingStub(channel);
		HelloWorld.Announcement referral = HelloWorld.Announcement.newBuilder().setKey("referred1").setMessage("referred1").setSignature("referred1").build();
		HelloWorld.Announcement referral2 = HelloWorld.Announcement.newBuilder().setKey("referred2").setMessage("referred2").setSignature("referred2").build();

		//HelloWorld.RegisterRequest request = HelloWorld.RegisterRequest.newBuilder().setKey("publickey5").setUsername("user5").build();
		//HelloWorld.Announcement post = HelloWorld.Announcement.newBuilder().setKey("publickey2").setMessage("message2").setSignature("signature2").addA(referral).addA(referral2).build();
		//HelloWorld.PostRequest request = HelloWorld.PostRequest.newBuilder().setPost(post).build();
		HelloWorld.Announcement post = HelloWorld.Announcement.newBuilder().setKey("publickey2").setMessage("message2").setSignature("signature2").addA(referral).addA(referral2).build();
		HelloWorld.PostGeneralRequest request = HelloWorld.PostGeneralRequest.newBuilder().setPost(post).build();

		// Finally, make the call using the stub
		HelloWorld.PostGeneralResponse response = stub.postGeneral(request);

		// HelloResponse has auto-generated toString method that shows its contents
		System.out.println(response);

		// A Channel should be shutdown before stopping the process.
		channel.shutdownNow();
	}

}
