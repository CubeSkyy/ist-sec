package com.dpas;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 * <pre>
 * Defining a Service, a Service can have multiple RPC operations
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.18.0)",
    comments = "Source: HelloWorld.proto")
public final class HelloWorldServiceGrpc {

  private HelloWorldServiceGrpc() {}

  public static final String SERVICE_NAME = "com.dpas.HelloWorldService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.dpas.HelloWorld.HelloRequest,
      com.dpas.HelloWorld.HelloResponse> getGreetingMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "greeting",
      requestType = com.dpas.HelloWorld.HelloRequest.class,
      responseType = com.dpas.HelloWorld.HelloResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.dpas.HelloWorld.HelloRequest,
      com.dpas.HelloWorld.HelloResponse> getGreetingMethod() {
    io.grpc.MethodDescriptor<com.dpas.HelloWorld.HelloRequest, com.dpas.HelloWorld.HelloResponse> getGreetingMethod;
    if ((getGreetingMethod = HelloWorldServiceGrpc.getGreetingMethod) == null) {
      synchronized (HelloWorldServiceGrpc.class) {
        if ((getGreetingMethod = HelloWorldServiceGrpc.getGreetingMethod) == null) {
          HelloWorldServiceGrpc.getGreetingMethod = getGreetingMethod = 
              io.grpc.MethodDescriptor.<com.dpas.HelloWorld.HelloRequest, com.dpas.HelloWorld.HelloResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "com.dpas.HelloWorldService", "greeting"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.dpas.HelloWorld.HelloRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.dpas.HelloWorld.HelloResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new HelloWorldServiceMethodDescriptorSupplier("greeting"))
                  .build();
          }
        }
     }
     return getGreetingMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.dpas.HelloWorld.RegisterRequest,
      com.dpas.HelloWorld.RegisterResponse> getRegisterMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "register",
      requestType = com.dpas.HelloWorld.RegisterRequest.class,
      responseType = com.dpas.HelloWorld.RegisterResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.dpas.HelloWorld.RegisterRequest,
      com.dpas.HelloWorld.RegisterResponse> getRegisterMethod() {
    io.grpc.MethodDescriptor<com.dpas.HelloWorld.RegisterRequest, com.dpas.HelloWorld.RegisterResponse> getRegisterMethod;
    if ((getRegisterMethod = HelloWorldServiceGrpc.getRegisterMethod) == null) {
      synchronized (HelloWorldServiceGrpc.class) {
        if ((getRegisterMethod = HelloWorldServiceGrpc.getRegisterMethod) == null) {
          HelloWorldServiceGrpc.getRegisterMethod = getRegisterMethod = 
              io.grpc.MethodDescriptor.<com.dpas.HelloWorld.RegisterRequest, com.dpas.HelloWorld.RegisterResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "com.dpas.HelloWorldService", "register"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.dpas.HelloWorld.RegisterRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.dpas.HelloWorld.RegisterResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new HelloWorldServiceMethodDescriptorSupplier("register"))
                  .build();
          }
        }
     }
     return getRegisterMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.dpas.HelloWorld.PostRequest,
      com.dpas.HelloWorld.PostResponse> getPostMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "post",
      requestType = com.dpas.HelloWorld.PostRequest.class,
      responseType = com.dpas.HelloWorld.PostResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.dpas.HelloWorld.PostRequest,
      com.dpas.HelloWorld.PostResponse> getPostMethod() {
    io.grpc.MethodDescriptor<com.dpas.HelloWorld.PostRequest, com.dpas.HelloWorld.PostResponse> getPostMethod;
    if ((getPostMethod = HelloWorldServiceGrpc.getPostMethod) == null) {
      synchronized (HelloWorldServiceGrpc.class) {
        if ((getPostMethod = HelloWorldServiceGrpc.getPostMethod) == null) {
          HelloWorldServiceGrpc.getPostMethod = getPostMethod = 
              io.grpc.MethodDescriptor.<com.dpas.HelloWorld.PostRequest, com.dpas.HelloWorld.PostResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "com.dpas.HelloWorldService", "post"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.dpas.HelloWorld.PostRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.dpas.HelloWorld.PostResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new HelloWorldServiceMethodDescriptorSupplier("post"))
                  .build();
          }
        }
     }
     return getPostMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.dpas.HelloWorld.PostGeneralRequest,
      com.dpas.HelloWorld.PostGeneralResponse> getPostGeneralMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "postGeneral",
      requestType = com.dpas.HelloWorld.PostGeneralRequest.class,
      responseType = com.dpas.HelloWorld.PostGeneralResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.dpas.HelloWorld.PostGeneralRequest,
      com.dpas.HelloWorld.PostGeneralResponse> getPostGeneralMethod() {
    io.grpc.MethodDescriptor<com.dpas.HelloWorld.PostGeneralRequest, com.dpas.HelloWorld.PostGeneralResponse> getPostGeneralMethod;
    if ((getPostGeneralMethod = HelloWorldServiceGrpc.getPostGeneralMethod) == null) {
      synchronized (HelloWorldServiceGrpc.class) {
        if ((getPostGeneralMethod = HelloWorldServiceGrpc.getPostGeneralMethod) == null) {
          HelloWorldServiceGrpc.getPostGeneralMethod = getPostGeneralMethod = 
              io.grpc.MethodDescriptor.<com.dpas.HelloWorld.PostGeneralRequest, com.dpas.HelloWorld.PostGeneralResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "com.dpas.HelloWorldService", "postGeneral"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.dpas.HelloWorld.PostGeneralRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.dpas.HelloWorld.PostGeneralResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new HelloWorldServiceMethodDescriptorSupplier("postGeneral"))
                  .build();
          }
        }
     }
     return getPostGeneralMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.dpas.HelloWorld.ReadRequest,
      com.dpas.HelloWorld.ReadResponse> getReadMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "read",
      requestType = com.dpas.HelloWorld.ReadRequest.class,
      responseType = com.dpas.HelloWorld.ReadResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.dpas.HelloWorld.ReadRequest,
      com.dpas.HelloWorld.ReadResponse> getReadMethod() {
    io.grpc.MethodDescriptor<com.dpas.HelloWorld.ReadRequest, com.dpas.HelloWorld.ReadResponse> getReadMethod;
    if ((getReadMethod = HelloWorldServiceGrpc.getReadMethod) == null) {
      synchronized (HelloWorldServiceGrpc.class) {
        if ((getReadMethod = HelloWorldServiceGrpc.getReadMethod) == null) {
          HelloWorldServiceGrpc.getReadMethod = getReadMethod = 
              io.grpc.MethodDescriptor.<com.dpas.HelloWorld.ReadRequest, com.dpas.HelloWorld.ReadResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "com.dpas.HelloWorldService", "read"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.dpas.HelloWorld.ReadRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.dpas.HelloWorld.ReadResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new HelloWorldServiceMethodDescriptorSupplier("read"))
                  .build();
          }
        }
     }
     return getReadMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.dpas.HelloWorld.ReadGeneralRequest,
      com.dpas.HelloWorld.ReadGeneralResponse> getReadGeneralMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "readGeneral",
      requestType = com.dpas.HelloWorld.ReadGeneralRequest.class,
      responseType = com.dpas.HelloWorld.ReadGeneralResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.dpas.HelloWorld.ReadGeneralRequest,
      com.dpas.HelloWorld.ReadGeneralResponse> getReadGeneralMethod() {
    io.grpc.MethodDescriptor<com.dpas.HelloWorld.ReadGeneralRequest, com.dpas.HelloWorld.ReadGeneralResponse> getReadGeneralMethod;
    if ((getReadGeneralMethod = HelloWorldServiceGrpc.getReadGeneralMethod) == null) {
      synchronized (HelloWorldServiceGrpc.class) {
        if ((getReadGeneralMethod = HelloWorldServiceGrpc.getReadGeneralMethod) == null) {
          HelloWorldServiceGrpc.getReadGeneralMethod = getReadGeneralMethod = 
              io.grpc.MethodDescriptor.<com.dpas.HelloWorld.ReadGeneralRequest, com.dpas.HelloWorld.ReadGeneralResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "com.dpas.HelloWorldService", "readGeneral"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.dpas.HelloWorld.ReadGeneralRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.dpas.HelloWorld.ReadGeneralResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new HelloWorldServiceMethodDescriptorSupplier("readGeneral"))
                  .build();
          }
        }
     }
     return getReadGeneralMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static HelloWorldServiceStub newStub(io.grpc.Channel channel) {
    return new HelloWorldServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static HelloWorldServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new HelloWorldServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static HelloWorldServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new HelloWorldServiceFutureStub(channel);
  }

  /**
   * <pre>
   * Defining a Service, a Service can have multiple RPC operations
   * </pre>
   */
  public static abstract class HelloWorldServiceImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Define a RPC operation
     * </pre>
     */
    public void greeting(com.dpas.HelloWorld.HelloRequest request,
        io.grpc.stub.StreamObserver<com.dpas.HelloWorld.HelloResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGreetingMethod(), responseObserver);
    }

    /**
     */
    public void register(com.dpas.HelloWorld.RegisterRequest request,
        io.grpc.stub.StreamObserver<com.dpas.HelloWorld.RegisterResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getRegisterMethod(), responseObserver);
    }

    /**
     */
    public void post(com.dpas.HelloWorld.PostRequest request,
        io.grpc.stub.StreamObserver<com.dpas.HelloWorld.PostResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getPostMethod(), responseObserver);
    }

    /**
     */
    public void postGeneral(com.dpas.HelloWorld.PostGeneralRequest request,
        io.grpc.stub.StreamObserver<com.dpas.HelloWorld.PostGeneralResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getPostGeneralMethod(), responseObserver);
    }

    /**
     */
    public void read(com.dpas.HelloWorld.ReadRequest request,
        io.grpc.stub.StreamObserver<com.dpas.HelloWorld.ReadResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getReadMethod(), responseObserver);
    }

    /**
     */
    public void readGeneral(com.dpas.HelloWorld.ReadGeneralRequest request,
        io.grpc.stub.StreamObserver<com.dpas.HelloWorld.ReadGeneralResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getReadGeneralMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getGreetingMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.dpas.HelloWorld.HelloRequest,
                com.dpas.HelloWorld.HelloResponse>(
                  this, METHODID_GREETING)))
          .addMethod(
            getRegisterMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.dpas.HelloWorld.RegisterRequest,
                com.dpas.HelloWorld.RegisterResponse>(
                  this, METHODID_REGISTER)))
          .addMethod(
            getPostMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.dpas.HelloWorld.PostRequest,
                com.dpas.HelloWorld.PostResponse>(
                  this, METHODID_POST)))
          .addMethod(
            getPostGeneralMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.dpas.HelloWorld.PostGeneralRequest,
                com.dpas.HelloWorld.PostGeneralResponse>(
                  this, METHODID_POST_GENERAL)))
          .addMethod(
            getReadMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.dpas.HelloWorld.ReadRequest,
                com.dpas.HelloWorld.ReadResponse>(
                  this, METHODID_READ)))
          .addMethod(
            getReadGeneralMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.dpas.HelloWorld.ReadGeneralRequest,
                com.dpas.HelloWorld.ReadGeneralResponse>(
                  this, METHODID_READ_GENERAL)))
          .build();
    }
  }

  /**
   * <pre>
   * Defining a Service, a Service can have multiple RPC operations
   * </pre>
   */
  public static final class HelloWorldServiceStub extends io.grpc.stub.AbstractStub<HelloWorldServiceStub> {
    private HelloWorldServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private HelloWorldServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected HelloWorldServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new HelloWorldServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Define a RPC operation
     * </pre>
     */
    public void greeting(com.dpas.HelloWorld.HelloRequest request,
        io.grpc.stub.StreamObserver<com.dpas.HelloWorld.HelloResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGreetingMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void register(com.dpas.HelloWorld.RegisterRequest request,
        io.grpc.stub.StreamObserver<com.dpas.HelloWorld.RegisterResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getRegisterMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void post(com.dpas.HelloWorld.PostRequest request,
        io.grpc.stub.StreamObserver<com.dpas.HelloWorld.PostResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getPostMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void postGeneral(com.dpas.HelloWorld.PostGeneralRequest request,
        io.grpc.stub.StreamObserver<com.dpas.HelloWorld.PostGeneralResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getPostGeneralMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void read(com.dpas.HelloWorld.ReadRequest request,
        io.grpc.stub.StreamObserver<com.dpas.HelloWorld.ReadResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getReadMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void readGeneral(com.dpas.HelloWorld.ReadGeneralRequest request,
        io.grpc.stub.StreamObserver<com.dpas.HelloWorld.ReadGeneralResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getReadGeneralMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * Defining a Service, a Service can have multiple RPC operations
   * </pre>
   */
  public static final class HelloWorldServiceBlockingStub extends io.grpc.stub.AbstractStub<HelloWorldServiceBlockingStub> {
    private HelloWorldServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private HelloWorldServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected HelloWorldServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new HelloWorldServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Define a RPC operation
     * </pre>
     */
    public com.dpas.HelloWorld.HelloResponse greeting(com.dpas.HelloWorld.HelloRequest request) {
      return blockingUnaryCall(
          getChannel(), getGreetingMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.dpas.HelloWorld.RegisterResponse register(com.dpas.HelloWorld.RegisterRequest request) {
      return blockingUnaryCall(
          getChannel(), getRegisterMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.dpas.HelloWorld.PostResponse post(com.dpas.HelloWorld.PostRequest request) {
      return blockingUnaryCall(
          getChannel(), getPostMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.dpas.HelloWorld.PostGeneralResponse postGeneral(com.dpas.HelloWorld.PostGeneralRequest request) {
      return blockingUnaryCall(
          getChannel(), getPostGeneralMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.dpas.HelloWorld.ReadResponse read(com.dpas.HelloWorld.ReadRequest request) {
      return blockingUnaryCall(
          getChannel(), getReadMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.dpas.HelloWorld.ReadGeneralResponse readGeneral(com.dpas.HelloWorld.ReadGeneralRequest request) {
      return blockingUnaryCall(
          getChannel(), getReadGeneralMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * Defining a Service, a Service can have multiple RPC operations
   * </pre>
   */
  public static final class HelloWorldServiceFutureStub extends io.grpc.stub.AbstractStub<HelloWorldServiceFutureStub> {
    private HelloWorldServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private HelloWorldServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected HelloWorldServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new HelloWorldServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Define a RPC operation
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.dpas.HelloWorld.HelloResponse> greeting(
        com.dpas.HelloWorld.HelloRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGreetingMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.dpas.HelloWorld.RegisterResponse> register(
        com.dpas.HelloWorld.RegisterRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getRegisterMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.dpas.HelloWorld.PostResponse> post(
        com.dpas.HelloWorld.PostRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getPostMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.dpas.HelloWorld.PostGeneralResponse> postGeneral(
        com.dpas.HelloWorld.PostGeneralRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getPostGeneralMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.dpas.HelloWorld.ReadResponse> read(
        com.dpas.HelloWorld.ReadRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getReadMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.dpas.HelloWorld.ReadGeneralResponse> readGeneral(
        com.dpas.HelloWorld.ReadGeneralRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getReadGeneralMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GREETING = 0;
  private static final int METHODID_REGISTER = 1;
  private static final int METHODID_POST = 2;
  private static final int METHODID_POST_GENERAL = 3;
  private static final int METHODID_READ = 4;
  private static final int METHODID_READ_GENERAL = 5;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final HelloWorldServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(HelloWorldServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GREETING:
          serviceImpl.greeting((com.dpas.HelloWorld.HelloRequest) request,
              (io.grpc.stub.StreamObserver<com.dpas.HelloWorld.HelloResponse>) responseObserver);
          break;
        case METHODID_REGISTER:
          serviceImpl.register((com.dpas.HelloWorld.RegisterRequest) request,
              (io.grpc.stub.StreamObserver<com.dpas.HelloWorld.RegisterResponse>) responseObserver);
          break;
        case METHODID_POST:
          serviceImpl.post((com.dpas.HelloWorld.PostRequest) request,
              (io.grpc.stub.StreamObserver<com.dpas.HelloWorld.PostResponse>) responseObserver);
          break;
        case METHODID_POST_GENERAL:
          serviceImpl.postGeneral((com.dpas.HelloWorld.PostGeneralRequest) request,
              (io.grpc.stub.StreamObserver<com.dpas.HelloWorld.PostGeneralResponse>) responseObserver);
          break;
        case METHODID_READ:
          serviceImpl.read((com.dpas.HelloWorld.ReadRequest) request,
              (io.grpc.stub.StreamObserver<com.dpas.HelloWorld.ReadResponse>) responseObserver);
          break;
        case METHODID_READ_GENERAL:
          serviceImpl.readGeneral((com.dpas.HelloWorld.ReadGeneralRequest) request,
              (io.grpc.stub.StreamObserver<com.dpas.HelloWorld.ReadGeneralResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class HelloWorldServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    HelloWorldServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.dpas.HelloWorld.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("HelloWorldService");
    }
  }

  private static final class HelloWorldServiceFileDescriptorSupplier
      extends HelloWorldServiceBaseDescriptorSupplier {
    HelloWorldServiceFileDescriptorSupplier() {}
  }

  private static final class HelloWorldServiceMethodDescriptorSupplier
      extends HelloWorldServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    HelloWorldServiceMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (HelloWorldServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new HelloWorldServiceFileDescriptorSupplier())
              .addMethod(getGreetingMethod())
              .addMethod(getRegisterMethod())
              .addMethod(getPostMethod())
              .addMethod(getPostGeneralMethod())
              .addMethod(getReadMethod())
              .addMethod(getReadGeneralMethod())
              .build();
        }
      }
    }
    return result;
  }
}
