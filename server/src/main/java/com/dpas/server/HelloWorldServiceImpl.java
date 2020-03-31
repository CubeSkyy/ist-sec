package com.dpas.server;

/* these are generated by the hello-world-server contract */

import com.dpas.HelloWorld;
import com.dpas.HelloWorldServiceGrpc;
import com.dpas.crypto.Main;
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
import com.google.protobuf.ByteString;
import java.security.*;
import java.security.cert.CertificateException;

public class HelloWorldServiceImpl extends HelloWorldServiceGrpc.HelloWorldServiceImplBase {

    /*--------------------------------------------------FILES---------------------------------------------------------*/
    public static final String USERS_FILE = "users.tmp";
    public static final String PARTICULAR_FILE = "particular.tmp";
    public static final String GENERAL_FILE = "general.tmp";
    public static final String POSTID_FILE = "postid.tmp";
    public static final String MSG_USERS = "Users successfully read/written to file.";
    public static final String MSG_PARTICULAR = "Particular post successfully read/written to file.";
    public static final String MSG_GENERAL = "General post successfully read/written to file.";
    public static final String MSG_POSTID = "Current post ID successfully read/written to file.";

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
                        writeToFile(new HashMap<String, ArrayList<Post>>(), PARTICULAR_FILE, MSG_PARTICULAR);
                        break;

                    case GENERAL_FILE:
                        writeToFile(new GeneralPost(), GENERAL_FILE, MSG_GENERAL);
                        break;

                    case POSTID_FILE:
                        writeToFile(1, POSTID_FILE, MSG_POSTID);
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

    /*-------------------------------------------------GREETING-------------------------------------------------------*/
    @Override
    public void greeting(HelloWorld.HelloRequest request, StreamObserver<HelloWorld.HelloResponse> responseObserver) {
        System.out.println(request);

        HelloWorld.HelloResponse response = HelloWorld.HelloResponse.newBuilder()
                .setGreeting("Hello " + request.getName()).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /*---------------------------------------------------TOKENS-------------------------------------------------------*/
    @Override
    public synchronized void getToken(HelloWorld.GetTokenRequest request, StreamObserver<HelloWorld.GetTokenResponse> responseObserver) {
        System.out.println(request);

        String key = request.getKey();

        try {
            if (!Main.hasCertificate(key)) { //key == userAlias
                Status status = Status.INVALID_ARGUMENT;
                status = status.withDescription("User is not registered in keystore.");
                responseObserver.onError(status.asRuntimeException());
            }
        } catch (FileNotFoundException fnfe){
            fnfe.printStackTrace();

        } catch (KeyStoreException kse){
            kse.printStackTrace();

        } catch (IOException ioe){
            ioe.printStackTrace();

        } catch (CertificateException ce){
            ce.printStackTrace();

        } catch (NoSuchAlgorithmException nsae){
            nsae.printStackTrace();

        }

        checkFile(USERS_FILE);

        HashMap<String, String> users = (HashMap<String, String>) readFromFile(USERS_FILE, MSG_USERS);

        if (!users.containsKey(key)) {
            Status status = Status.INVALID_ARGUMENT;
            status = status.withDescription("User is not registered");
            responseObserver.onError(status.asRuntimeException());
        }

        String token = RandomStringUtils.randomAlphanumeric(10);

        users.replace(key, token);
        writeToFile(users, USERS_FILE, MSG_USERS);

        System.out.println("Users: " + users);

        HelloWorld.GetTokenResponse response = HelloWorld.GetTokenResponse.newBuilder()
                .setToken(token).build();

        Timer timer = new Timer(30000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                checkFile(USERS_FILE);

                HashMap<String, String> users = (HashMap<String, String>) readFromFile(USERS_FILE, MSG_USERS);

                if (users.get(key).equals(token)) {

                    users.replace(key, null);
                    writeToFile(users, USERS_FILE, MSG_USERS);

                    System.out.println("User token expired: " + users);
                }
            }
        });
        timer.setRepeats(false);
        timer.start();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /*----------------------------------------------------------------------------------------------------------------*/
    /*------------------------------------------------COMMANDS--------------------------------------------------------*/
    /*----------------------------------------------------------------------------------------------------------------*/
    @Override
    public synchronized void register(HelloWorld.RegisterRequest request, StreamObserver<HelloWorld.RegisterResponse> responseObserver) {
        System.out.println(request);

        String key = request.getKey();
        try {
            if (!Main.hasCertificate(key)) { //key == userAlias
                Status status = Status.INVALID_ARGUMENT;
                status = status.withDescription("User is not registered in keystore.");
                responseObserver.onError(status.asRuntimeException());
            }
        } catch (FileNotFoundException fnfe){
            fnfe.printStackTrace();

        } catch (KeyStoreException kse){
            kse.printStackTrace();

        } catch (IOException ioe){
            ioe.printStackTrace();

        } catch (CertificateException ce){
            ce.printStackTrace();

        } catch (NoSuchAlgorithmException nsae){
            nsae.printStackTrace();

        }

        checkFile(USERS_FILE);

        HashMap<String, String> users = (HashMap<String, String>) readFromFile(USERS_FILE, MSG_USERS);

        if (!users.containsKey(key)) {
            users.put(key, null);
            writeToFile(users, USERS_FILE, MSG_USERS);
        } else
            System.out.println("User is already registered.");

        System.out.println("Users: " + users);

        HelloWorld.RegisterResponse response = HelloWorld.RegisterResponse.newBuilder()
                .setResult(true).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /*--------------------------------------------------POSTS---------------------------------------------------------*/
    @Override
    public synchronized void post(HelloWorld.PostRequest request, StreamObserver<HelloWorld.PostResponse> responseObserver) {
        System.out.println(request);

        HelloWorld.Announcement post = request.getPost();
        String key = post.getKey();
        String message = post.getMessage();

        if (message.length() > 255) {
            Status status = Status.INVALID_ARGUMENT;
            status = status.withDescription("Invalid message length. Message needs to be smaller than 255 characters.");
            responseObserver.onError(status.asRuntimeException());
        }

        checkFile(USERS_FILE);

        HashMap<String, String> users = (HashMap<String, String>) readFromFile(USERS_FILE, MSG_USERS);

        if (!users.containsKey(key)) {
            Status status = Status.INVALID_ARGUMENT;
            status = status.withDescription("User is not registered");
            responseObserver.onError(status.asRuntimeException());
        }


        Post postToBoard = new Post();

        /*-------------------------------SIGNATURE AND HASH---------------------------------*/
        ByteString sigByteString = request.getSignature();
        ByteString hashByteString = request.getHash();

        byte[] signature = sigByteString.toByteArray();
        byte[] hash = hashByteString.toByteArray();

        try{
            byte[] messageHash = Main.getHashFromObject(message);
            Main.validate(signature, key, messageHash, hash); //key == userAlias
        } catch (Exception e){
            e.printStackTrace();
        }

        postToBoard.setSignature(signature);
        postToBoard.setHash(hash);

        /*------------------------------------POST ID---------------------------------------*/
        // TODO remove token from file

        checkFile(PARTICULAR_FILE);
        checkFile(POSTID_FILE);

        int postId = (Integer) readFromFile(POSTID_FILE, MSG_POSTID);
        writeToFile(postId+1, POSTID_FILE, MSG_POSTID);

        /*----------------------BUILD ANNOUNCEMENTS OF THE POST-----------------------------*/
        HelloWorld.Announcement.Builder postBuilder = post.toBuilder();
        postBuilder.setPostId(postId);
        postBuilder.setToken("");
        post = postBuilder.build();

        HashMap<String, Post> particular = (HashMap<String, Post>) readFromFile(PARTICULAR_FILE, MSG_PARTICULAR);

        if (!particular.containsKey(key)) {
            ArrayList<HelloWorld.Announcement> tmp = new ArrayList<HelloWorld.Announcement>();
            tmp.add(post);
            postToBoard.setPosts(tmp);
            particular.put(key, postToBoard);

        } else {
            ArrayList<HelloWorld.Announcement> tmp = particular.get(key).getPosts();
            tmp.add(post);
            postToBoard.setPosts(tmp);
            particular.replace(key, postToBoard);
        }

        writeToFile(particular, PARTICULAR_FILE, MSG_PARTICULAR);
        System.out.println("Particular Posts: " + particular);

        HelloWorld.PostResponse response = HelloWorld.PostResponse.newBuilder()
                .setResult(true).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }


    @Override
    public synchronized void postGeneral(HelloWorld.PostGeneralRequest request, StreamObserver<HelloWorld.PostGeneralResponse> responseObserver) {
        System.out.println(request);

        HelloWorld.Announcement post = request.getPost();
        String key = post.getKey();
        String message = post.getMessage();

        if (message.length() > 255) {
            Status status = Status.INVALID_ARGUMENT;
            status = status.withDescription("Invalid message length. Message needs to be smaller than 255 characters.");
            responseObserver.onError(status.asRuntimeException());
        }

        checkFile(USERS_FILE);

        HashMap<String, String> users = (HashMap<String, String>) readFromFile(USERS_FILE, MSG_USERS);

        if (!users.containsKey(key)) {
            Status status = Status.INVALID_ARGUMENT;
            status = status.withDescription("User is not registered");
            responseObserver.onError(status.asRuntimeException());
        }


        GeneralPost general = (GeneralPost) readFromFile(GENERAL_FILE, MSG_GENERAL);
        Post postToBoard = new Post();

        /*-----------------------SIGNATURE AND HASH FROM THE USER---------------------------*/
        ByteString sigByteString = request.getSignature();
        ByteString hashByteString = request.getHash();

        byte[] signature = sigByteString.toByteArray();
        byte[] hash = hashByteString.toByteArray();

        try{
            byte[] messageHash = Main.getHashFromObject(message);

            Main.validate(signature, key, messageHash, hash); //key == userAlias

            postToBoard.setSignature(signature);
            postToBoard.setHash(hash);
        } catch (Exception e){
            e.printStackTrace();
        }

        /*------------------------------------POST ID---------------------------------------*/
        // TODO remove  token from file

        checkFile(GENERAL_FILE);
        checkFile(POSTID_FILE);

        int postId = (Integer) readFromFile(POSTID_FILE, MSG_POSTID);
        writeToFile(postId+1, POSTID_FILE, MSG_POSTID);

        /*----------------------BUILD ANNOUNCEMENTS OF THE POST-----------------------------*/
        HelloWorld.Announcement.Builder postBuilder = post.toBuilder();
        postBuilder.setPostId(postId);
        postBuilder.setToken("");
        post = postBuilder.build();

        postToBoard.addPosts(post);
        general.addPosts(postToBoard);

        /*----------------------SIGNATURE AND HASH FROM THE SERVER--------------------------*/
        try {
            byte[] hashGeneral = Main.getHashFromObject(general.getPosts());
            byte[] sigGeneral = Main.getSignature(hashGeneral, "server1"); //TODO change to serverAlias when we have multiple servers

            general.setSignature(sigGeneral);
            general.setHash(hashGeneral);
        } catch (Exception e){
            e.printStackTrace();
        }

        writeToFile(general, GENERAL_FILE, MSG_GENERAL);

        System.out.println("General posts: " + general);

        HelloWorld.PostGeneralResponse response = HelloWorld.PostGeneralResponse.newBuilder()
                .setResult(true).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    private List<HelloWorld.Announcement> getPost(List<Integer> ids){
        checkFile(GENERAL_FILE);

        GeneralPost generalPost = (GeneralPost) readFromFile(GENERAL_FILE, MSG_GENERAL);
        ArrayList<HelloWorld.Announcement> general = new ArrayList<HelloWorld.Announcement>();

        for(Post post: generalPost.getPosts()){
            for(HelloWorld.Announcement announcement : post.getPosts()){
                general.add(announcement);
            }
        }

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
        HashMap<String, Post> particular = (HashMap<String, Post>) readFromFile(PARTICULAR_FILE, MSG_PARTICULAR);

        for (Map.Entry<String, Post> entry : particular.entrySet()){
            for(HelloWorld.Announcement announcement : entry.getValue().getPosts()){
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

    /*--------------------------------------------------READS---------------------------------------------------------*/
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

        HashMap<String, Post> particular = (HashMap<String, Post>) readFromFile(PARTICULAR_FILE, MSG_PARTICULAR);
        System.out.println("Particular Posts from File: " + particular);

        if (!particular.containsKey(key)) {
            Status status = Status.INVALID_ARGUMENT;
            status = status.withDescription("Invalid key. There is no user with the specified key.");
            responseObserver.onError(status.asRuntimeException());
        } else {

            Post postFromBoard = particular.get(key);
            ArrayList<HelloWorld.Announcement> tmp = postFromBoard.getPosts();
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
            byte[] signature = particular.get(key).getSignature();
            byte[] hash = particular.get(key).getHash();

            ByteString sigByteString = ByteString.copyFrom(signature);
            ByteString hashByteString = ByteString.copyFrom(hash);

            HelloWorld.ReadResponse response = HelloWorld.ReadResponse.newBuilder().addAllResult(result).setSignature(sigByteString).setHash(hashByteString).build();
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

        GeneralPost postGeneral = (GeneralPost) readFromFile(GENERAL_FILE, MSG_GENERAL);
        ArrayList<Post> general = postGeneral.getPosts();
        ArrayList<Post> result = new ArrayList<Post>();

        /*-------------------------------SIGNATURE AND HASH---------------------------------*/
        //to validate if everything is as it was when written

        byte[] generalSignature = postGeneral.getSignature();
        byte[] generalHash = postGeneral.getHash();

        try{
            byte[] postsHash = Main.getHashFromObject(general);
            Main.validate(generalSignature, "server1", postsHash, generalHash); //TODO change when we get multiple servers to serverAlias
        } catch (Exception e){
            e.printStackTrace();
        }
        /*----------------------------------------------------------------------------------*/

        // TODO resolve post ID into a real post and add it to the response

        if (number > 0){
            ListIterator<Post> listIter = general.listIterator(general.size());
            for(int i = 0; i < number; i++){
               result.add(listIter.previous());
            }

		} else {
            Collections.reverse(general);
			result.addAll(general);
		}

        // resolve post IDs into real posts and add them to the response
        ArrayList<HelloWorld.Announcement> resultToRead = new ArrayList<HelloWorld.Announcement>();

        for (int i=0; i < result.size(); i++) {
            for(int j=0; j < result.get(i).getPosts().size(); j++) {
                resultToRead.add(result.get(i).getPosts().get(j));

                List<HelloWorld.Announcement> currPostRefs = resultToRead.get(i+j).getAList();

                //TODO give postIDs to the ones in the AList
                if (currPostRefs.size() > 0) {
                    HelloWorld.Announcement.Builder currPostBuilder = resultToRead.get(i+j).toBuilder();

                    List<Integer> ids = new ArrayList<Integer>();

                    for (HelloWorld.Announcement ref : currPostRefs) {
                        ids.add(ref.getPostId());
                        System.out.println("ref.getPostId(): " + ref.getPostId());
                    }

                    List<HelloWorld.Announcement> updatedPosts = getPost(ids);

                    currPostBuilder.clearA().addAllA(updatedPosts);
                    HelloWorld.Announcement currPost = currPostBuilder.build();
                    System.out.println("currPost SENT TO READ: " + currPost);

                    resultToRead.set(i, currPost);

                }
            }
        }
        /*-----------------------------NEW SIGNATURE AND HASH-------------------------------*/
        try {
            byte[] hashGeneral = Main.getHashFromObject(resultToRead);
            byte[] sigGeneral = Main.getSignature(hashGeneral, "server1"); //TODO change to serverAlias when we have multiple servers

            ByteString sigByteString = ByteString.copyFrom(sigGeneral);
            ByteString hashByteString = ByteString.copyFrom(hashGeneral);

            HelloWorld.ReadGeneralResponse response = HelloWorld.ReadGeneralResponse.newBuilder()
                    .addAllResult(resultToRead).setSignature(sigByteString).setHash(hashByteString).build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

}
