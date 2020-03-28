package com.dpas.server;

/* these are generated by the hello-world-server contract */

import com.dpas.HelloWorld;
import com.dpas.HelloWorldServiceGrpc;
import io.grpc.Grpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;

public class HelloWorldServiceImpl extends HelloWorldServiceGrpc.HelloWorldServiceImplBase {

    public static final String USERS_FILE = "users.tmp";
    public static final String PARTICULAR_FILE = "particular.tmp";
    public static final String GENERAL_FILE = "general.tmp";
    public static final String MSG_USERS = "Users successfully read/written to file.";
    public static final String MSG_PARTICULAR = "Particular post successfully read/written to file.";
    public static final String MSG_GENERAL = "General post successfully read/written to file.";

    // TODO persist post ID
    public static int postId = 1;

    public void checkFile(String filename) {

        File f = new File(filename);
        System.out.println("File " + filename + " exists: " + f.isFile());

        if (!f.isFile()) {
            try {
                f.createNewFile();
                switch (filename) {
                    case USERS_FILE:
                        writeToFile(new HashMap<String, String>(), USERS_FILE, MSG_USERS);
                        break;

                    case PARTICULAR_FILE:
                        writeToFile(new HashMap<String, ArrayList<HelloWorld.Announcement>>(), PARTICULAR_FILE, MSG_PARTICULAR);
                        break;

                    case GENERAL_FILE:
                        writeToFile(new ArrayList<HelloWorld.Announcement>(), GENERAL_FILE, MSG_GENERAL);
                        break;

                    default:
                        System.err.println("Invalid filename. Could not write to file.");
                        break;
                }

                System.out.println("" + filename + " file not found. New instance has been created.");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void writeToFile(Object users, String type, String msg) {

        try {
            FileOutputStream fos = new FileOutputStream(type+"Backup");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(users);
            System.out.println(msg);
            oos.close();
            Files.move(Paths.get(type+"Backup"), Paths.get(type), StandardCopyOption.ATOMIC_MOVE);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Object readFromFile(String type, String msg) {

        try {
            FileInputStream fis = new FileInputStream(type);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Object users = ois.readObject();
            System.out.println(msg);
            ois.close();
            return users;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Object();
    }

    @Override
    public void greeting(HelloWorld.HelloRequest request, StreamObserver<HelloWorld.HelloResponse> responseObserver) {

        // HelloRequest has auto-generated toString method that shows its contents
        System.out.println(request);

        // You must use a builder to construct a new Protobuffer object
        HelloWorld.HelloResponse response = HelloWorld.HelloResponse.newBuilder()
                .setGreeting("Hello " + request.getName()).build();

        // Use responseObserver to send a single response back
        responseObserver.onNext(response);

        // When you are done, you must call onCompleted
        responseObserver.onCompleted();
    }

    @Override
    public synchronized void register(HelloWorld.RegisterRequest request, StreamObserver<HelloWorld.RegisterResponse> responseObserver) {

        // HelloRequest has auto-generated toString method that shows its contents
        System.out.println(request);

        String key = request.getKey();

		/*
			TODO check if public key is owned by user with a digital signature
		*/

        checkFile(USERS_FILE);

        HashMap<String, String> users = (HashMap<String, String>) readFromFile(USERS_FILE, MSG_USERS);
        System.out.println("Class of retrieved info: " + users.getClass().getName());

        if (!users.containsKey(key)) {

            users.put(key, null);
            writeToFile(users, USERS_FILE, MSG_USERS);
        } else
            System.out.println("User is already registered.");

        System.out.println("Users: " + users);


        // You must use a builder to construct a new Protobuffer object
        HelloWorld.RegisterResponse response = HelloWorld.RegisterResponse.newBuilder()
                .setResult(true).build();

        // Use responseObserver to send a single response back
        responseObserver.onNext(response);

        // When you are done, you must call onCompleted
        responseObserver.onCompleted();
    }

    @Override
    public synchronized void getToken(HelloWorld.GetTokenRequest request, StreamObserver<HelloWorld.GetTokenResponse> responseObserver) {
        // HelloRequest has auto-generated toString method that shows its contents
        System.out.println(request);

        String key = request.getKey();

		/*
			TODO check if public key is owned by user with a digital signature
		*/

        checkFile(USERS_FILE);

        HashMap<String, String> users = (HashMap<String, String>) readFromFile(USERS_FILE, MSG_USERS);
        System.out.println("Class of retrieved info: " + users.getClass().getName());

        if (!users.containsKey(key)) {
            Status status = Status.INVALID_ARGUMENT;
            status = status.withDescription("User is not registered");
            responseObserver.onError(status.asRuntimeException());
        }

        String token = RandomStringUtils.randomAlphanumeric(10);

        users.replace(key, token);
        writeToFile(users, USERS_FILE, MSG_USERS);

        System.out.println("Users: " + users);


        // You must use a builder to construct a new Protobuffer object
        HelloWorld.GetTokenResponse response = HelloWorld.GetTokenResponse.newBuilder()
                .setToken(token).build();

        Timer timer = new Timer(30000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                checkFile(USERS_FILE);

                HashMap<String, String> users = (HashMap<String, String>) readFromFile(USERS_FILE, MSG_USERS);
                System.out.println("Class of retrieved info: " + users.getClass().getName());

                if (users.get(key).equals(token)) {

                    users.replace(key, null);
                    writeToFile(users, USERS_FILE, MSG_USERS);

                    System.out.println("User token expired: " + users);
                }
            }
        });
        timer.setRepeats(false);
        timer.start();


        // Use responseObserver to send a single response back
        responseObserver.onNext(response);

        // When you are done, you must call onCompleted
        responseObserver.onCompleted();
    }

    @Override
    public synchronized void post(HelloWorld.PostRequest request, StreamObserver<HelloWorld.PostResponse> responseObserver) {

        // HelloRequest has auto-generated toString method that shows its contents
        System.out.println(request);

        HelloWorld.Announcement post = request.getPost();
        //List<HelloWorld.Announcement> a = post.getAList();
        System.out.println("A: " + post.getAList());

        String key = post.getKey();
        String message = post.getMessage();

        if (message.length() > 255) {
            Status status = Status.INVALID_ARGUMENT;
            status = status.withDescription("Invalid message length. Message needs to be smaller than 255 characters.");
            responseObserver.onError(status.asRuntimeException());
        }

        checkFile(USERS_FILE);

        HashMap<String, String> users = (HashMap<String, String>) readFromFile(USERS_FILE, MSG_USERS);
        System.out.println("Class of retrieved info: " + users.getClass().getName());

        if (!users.containsKey(key)) {
            Status status = Status.INVALID_ARGUMENT;
            status = status.withDescription("User is not registered");
            responseObserver.onError(status.asRuntimeException());
        }

        // TODO check if signature corresponds to message+announcement+token
        // TODO remove token from file

        checkFile(PARTICULAR_FILE);

        HelloWorld.Announcement.Builder postBuilder = post.toBuilder();
        postBuilder.setPostId(postId);
        postBuilder.setToken("");
        postId++;
        post = postBuilder.build();

        HashMap<String, ArrayList<HelloWorld.Announcement>> particular = (HashMap<String, ArrayList<HelloWorld.Announcement>>) readFromFile(PARTICULAR_FILE, MSG_PARTICULAR);
        System.out.println("Class of retrieved info: " + particular.getClass().getName());

        if (!particular.containsKey(key)) {
            ArrayList<HelloWorld.Announcement> tmp = new ArrayList<HelloWorld.Announcement>();
            tmp.add(post);
            particular.put(key, tmp);
        } else {
            ArrayList<HelloWorld.Announcement> tmp = particular.get(key);
            tmp.add(post);
            particular.replace(key, tmp);
        }

        writeToFile(particular, PARTICULAR_FILE, MSG_PARTICULAR);

        System.out.println("Particular posts: " + particular);

        // You must use a builder to construct a new Protobuffer object
        HelloWorld.PostResponse response = HelloWorld.PostResponse.newBuilder()
                .setResult(true).build();

        // Use responseObserver to send a single response back
        responseObserver.onNext(response);

        // When you are done, you must call onCompleted
        responseObserver.onCompleted();
    }


    @Override
    public synchronized void postGeneral(HelloWorld.PostGeneralRequest request, StreamObserver<HelloWorld.PostGeneralResponse> responseObserver) {

        // HelloRequest has auto-generated toString method that shows its contents
        System.out.println(request);

        HelloWorld.Announcement post = request.getPost();
        //List<HelloWorld.Announcement> a = post.getAList();
        System.out.println("A: " + post.getAList());

        String key = post.getKey();
        String message = post.getMessage();

        if (message.length() > 255) {
            Status status = Status.INVALID_ARGUMENT;
            status = status.withDescription("Invalid message length. Message needs to be smaller than 255 characters.");
            responseObserver.onError(status.asRuntimeException());
        }

        checkFile(USERS_FILE);

        HashMap<String, String> users = (HashMap<String, String>) readFromFile(USERS_FILE, MSG_USERS);
        System.out.println("Class of retrieved info: " + users.getClass().getName());

        if (!users.containsKey(key)) {
            Status status = Status.INVALID_ARGUMENT;
            status = status.withDescription("User is not registered");
            responseObserver.onError(status.asRuntimeException());
        }

        // TODO check if signature corresponds to message+announcement+token
        // TODO remove  token from file

        checkFile(GENERAL_FILE);

        HelloWorld.Announcement.Builder postBuilder = post.toBuilder();
        postBuilder.setPostId(postId);
        postBuilder.setToken("");
        postId++;
        post = postBuilder.build();

        ArrayList<HelloWorld.Announcement> general = (ArrayList<HelloWorld.Announcement>) readFromFile(GENERAL_FILE, MSG_GENERAL);
        System.out.println("Class of retrieved info: " + general.getClass().getName());

        general.add(post);

        writeToFile(general, GENERAL_FILE, MSG_GENERAL);

        System.out.println("General posts: " + general);


        // You must use a builder to construct a new Protobuffer object
        HelloWorld.PostGeneralResponse response = HelloWorld.PostGeneralResponse.newBuilder()
                .setResult(true).build();

        // Use responseObserver to send a single response back
        responseObserver.onNext(response);

        // When you are done, you must call onCompleted
        responseObserver.onCompleted();

    }


    @Override
    public synchronized void read(HelloWorld.ReadRequest request, StreamObserver<HelloWorld.ReadResponse> responseObserver) {

        String key = request.getKey();
        int number = request.getNumber();

        if (number < 0) {
            Status status = Status.INVALID_ARGUMENT;
            status = status.withDescription("Invalid message number. Number needs to be positive.");
            responseObserver.onError(status.asRuntimeException());
        }

        checkFile(PARTICULAR_FILE);

        HashMap<String, ArrayList<HelloWorld.Announcement>> particular = (HashMap<String, ArrayList<HelloWorld.Announcement>>) readFromFile(PARTICULAR_FILE, MSG_PARTICULAR);

        if (!particular.containsKey(key)) {
            Status status = Status.INVALID_ARGUMENT;
            status = status.withDescription("Invalid key. There is no user with the specified key.");
            responseObserver.onError(status.asRuntimeException());
        } else {

            ArrayList<HelloWorld.Announcement> tmp = particular.get(key);
            ArrayList<HelloWorld.Announcement> result = new ArrayList<HelloWorld.Announcement>();

			if (number > 0){
                ListIterator<HelloWorld.Announcement> listIter = tmp.listIterator(tmp.size());
                for(int i = 0; i < number; i++){
                    result.add(listIter.previous());
                }

			} else {
                Collections.reverse(tmp);
				result.addAll(tmp);
			}

			// resolve post IDs into real posts and add them to the response

            for (int i=0; i < result.size(); i++) {

                List<HelloWorld.Announcement> currPostRefs = result.get(i).getAList();

                if (currPostRefs.size() > 0) {
                    HelloWorld.Announcement.Builder currPostBuilder = result.get(i).toBuilder();

                    List<Integer> ids = new ArrayList<Integer>();

                    for (HelloWorld.Announcement ref: currPostRefs) {
                        ids.add(ref.getPostId());
                    }

                    List<HelloWorld.Announcement> updatedPosts = getPost(ids);

                    currPostBuilder.clearA().addAllA(updatedPosts);
                    HelloWorld.Announcement currPost = currPostBuilder.build();
                    result.set(i, currPost);

                }
            }

            // TODO sign response with the private key of the server

            HelloWorld.ReadResponse response = HelloWorld.ReadResponse.newBuilder().addAllResult(result).build();
            responseObserver.onNext(response);

        }

        responseObserver.onCompleted();
    }

    public synchronized void readGeneral(HelloWorld.ReadGeneralRequest request, StreamObserver<HelloWorld.ReadGeneralResponse> responseObserver) {

        int number = request.getNumber();

        if (number < 0) {
            Status status = Status.INVALID_ARGUMENT;
            status = status.withDescription("Invalid message number. Number needs to be positive.");
            responseObserver.onError(status.asRuntimeException());
        }

        checkFile(GENERAL_FILE);

        ArrayList<HelloWorld.Announcement> general = (ArrayList<HelloWorld.Announcement>) readFromFile(GENERAL_FILE, MSG_GENERAL);
        ArrayList<HelloWorld.Announcement> result = new ArrayList<HelloWorld.Announcement>();

        // TODO resolve post ID into a real post and add it to the response

        if (number > 0){
            ListIterator<HelloWorld.Announcement> listIter = general.listIterator(general.size());
            for(int i = 0; i < number; i++){
               result.add(listIter.previous());
            }

		} else {
            Collections.reverse(general);
			result.addAll(general);
		}

        // resolve post IDs into real posts and add them to the response

        for (int i=0; i < result.size(); i++) {

            List<HelloWorld.Announcement> currPostRefs = result.get(i).getAList();

            if (currPostRefs.size() > 0) {
                HelloWorld.Announcement.Builder currPostBuilder = result.get(i).toBuilder();

                List<Integer> ids = new ArrayList<Integer>();

                for (HelloWorld.Announcement ref: currPostRefs) {
                    ids.add(ref.getPostId());
                }

                List<HelloWorld.Announcement> updatedPosts = getPost(ids);

                currPostBuilder.clearA().addAllA(updatedPosts);
                HelloWorld.Announcement currPost = currPostBuilder.build();
                result.set(i, currPost);

            }
        }

        // TODO sign response with the private key of the server

        HelloWorld.ReadGeneralResponse response = HelloWorld.ReadGeneralResponse.newBuilder().addAllResult(result).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private List<HelloWorld.Announcement> getPost(List<Integer> ids){
        checkFile(GENERAL_FILE);

        ArrayList<HelloWorld.Announcement> general = (ArrayList<HelloWorld.Announcement>) readFromFile(GENERAL_FILE, MSG_GENERAL);
        List<HelloWorld.Announcement> result = new ArrayList<HelloWorld.Announcement>();

        for(HelloWorld.Announcement ann : general) {
            if (ids.contains(ann.getPostId())) {
                result.add(ann);
                ids.removeAll(Arrays.asList(ann.getPostId()));
                if (ids.size() == 0)
                    return result;
            }
        }
        checkFile(PARTICULAR_FILE);
        HashMap<String, ArrayList<HelloWorld.Announcement>> particular = (HashMap<String, ArrayList<HelloWorld.Announcement>>) readFromFile(PARTICULAR_FILE, MSG_PARTICULAR);

            for (Map.Entry<String, ArrayList<HelloWorld.Announcement>> entry : particular.entrySet()){
                for(HelloWorld.Announcement announcement : entry.getValue()){
                    if(ids.contains(announcement.getPostId())){
                        result.add(announcement);
                        ids.removeAll(Arrays.asList(announcement.getPostId()));
                        if (ids.size() == 0)
                            return result;
                    }
                }
            }


        return result;
    }

}
