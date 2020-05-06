package com.dpas.server;

import com.dpas.Dpas.*;
import com.dpas.DpasServiceGrpc;
import com.dpas.crypto.Main;
import com.google.protobuf.ByteString;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import jdk.nashorn.internal.runtime.regexp.joni.exception.ValueException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.ListIterator;

import static com.dpas.server.ServerDataStore.*;

public class DpasServiceImpl extends DpasServiceGrpc.DpasServiceImplBase {

    public static DpasServiceImpl instance = null;
    private HashMap<String, String> usersMap;
    private HashMap<String, ArrayList<Announcement>> particularMap;
    private ArrayList<Announcement> generalMap;
    private int postId;
    private int port;
    private int timestamp;
    private final String USERS_FILE;
    private final String PARTICULAR_FILE;
    private final String GENERAL_FILE;
    private final String POSTID_FILE;


    /*--------------------------------------------------FILES---------------------------------------------------------*/
    private DpasServiceImpl(int p) {
        port = p;
        USERS_FILE = COMMON_USERS_FILE + port;
        PARTICULAR_FILE = COMMON_PARTICULAR_FILE + port;
        GENERAL_FILE = COMMON_GENERAL_FILE + port;
        POSTID_FILE = COMMON_POSTID_FILE + port;
        initialize();
        timestamp = -1;
    }

    public static DpasServiceImpl getInstance(int port) {
        if (instance == null) {

            instance = new DpasServiceImpl(port);
        }
        return instance;
    }

    private void initialize() {
        checkFile(USERS_FILE);
        usersMap = (HashMap<String, String>) readFromFile(USERS_FILE, MSG_USERS);

        checkFile(PARTICULAR_FILE);
        particularMap = (HashMap<String, ArrayList<Announcement>>) readFromFile(PARTICULAR_FILE, MSG_PARTICULAR);

        checkFile(GENERAL_FILE);
        generalMap = (ArrayList<Announcement>) readFromFile(GENERAL_FILE, MSG_GENERAL);

        checkFile(POSTID_FILE);
        postId = (Integer) readFromFile(POSTID_FILE, MSG_POSTID);
    }

    private HashMap<String, String> getUsersMap() {
        return usersMap;
    }

    private HashMap<String, ArrayList<Announcement>> getParticularMap() {
        return particularMap;
    }

    private ArrayList<Announcement> getGeneralMap() {
        return generalMap;
    }

    private int getPostId() {
        return postId;
    }


    public void checkFile(String filename) {

        File f = new File(filename);
        System.out.println("File " + filename + " exists: " + f.isFile());

        if (!f.isFile()) {
            try {
                f.createNewFile();

                if (filename.equals(USERS_FILE)) writeToFile(new HashMap<String, String>(), USERS_FILE, MSG_USERS);
                else if (filename.equals(PARTICULAR_FILE))
                    writeToFile(new HashMap<String, ArrayList<Announcement>>(), PARTICULAR_FILE, MSG_PARTICULAR);
                else if (filename.equals(GENERAL_FILE))
                    writeToFile(new ArrayList<Announcement>(), GENERAL_FILE, MSG_GENERAL);
                else if (filename.equals(POSTID_FILE)) writeToFile(0, POSTID_FILE, MSG_POSTID);
                else System.err.println("Invalid filename. Could not write to file.");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void writeToFile(Object users, String type, String msg) {

        try {
            FileOutputStream fos = new FileOutputStream(type + "Backup");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(users);
            System.out.println(msg);
            oos.close();
            Files.move(Paths.get(type + "Backup"), Paths.get(type), StandardCopyOption.ATOMIC_MOVE);

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

    private void sendArgumentError(StreamObserver<?> responseObserver, String errorMessage) {
        Status status = Status.INVALID_ARGUMENT;
        status = status.withDescription(errorMessage);
        System.err.println(errorMessage);
        responseObserver.onError(status.asRuntimeException());
        responseObserver.onCompleted();
    }


    private void sendArgumentError(boolean condition, StreamObserver<?> responseObserver, String errorMessage) {
        if (condition) {
            Status status = Status.INVALID_ARGUMENT;
            status = status.withDescription(errorMessage);
            System.err.println(errorMessage);
            responseObserver.onError(status.asRuntimeException());
            responseObserver.onCompleted();
        }
    }


    /*---------------------------------------------------TOKENS-------------------------------------------------------*/
    @Override
    public synchronized void getToken(GetTokenRequest request, StreamObserver<GetTokenResponse> responseObserver) {
        System.out.println("Get Token Request Received: " + request);

        String key = request.getKey();

        /*--------------------------SIGNATURE AND HASH FROM USER----------------------------*/
        ByteString tokenSigByteString = request.getSignature();

        byte[] tokenSig = tokenSigByteString.toByteArray();

        try {
            byte[] keyHash = Main.getHashFromObject(key);
            boolean valid = Main.validate(tokenSig, key, keyHash); //key == userAlias
            sendArgumentError(!valid, responseObserver, MSG_ERROR_GETTOKEN_SIG);
        } catch (Exception e) {
            e.printStackTrace();
        }
        /*----------------------------------------------------------------------------------*/
        boolean valid = Main.hasCertificate(key);
        sendArgumentError(!valid, responseObserver, MSG_ERROR_CERTIFICATE);

        boolean validRegister = getUsersMap().containsKey(key);
        sendArgumentError(!validRegister, responseObserver, MSG_ERROR_NOT_REGISTERED);

        String token = RandomStringUtils.randomAlphanumeric(32);

        getUsersMap().replace(key, token);
        writeToFile(getUsersMap(), USERS_FILE, MSG_USERS);

        System.out.println("Users: " + usersMap);

        /*---------------------------SIGNATURE AND HASH FROM SERVER-------------------------*/

        try {
            byte[] hashServer = Main.getHashFromObject(token);
            byte[] sigServer = Main.getSignature(hashServer, "server1");

            ByteString sigServerByteString = ByteString.copyFrom(sigServer);

            /*----------------------------------------------------------------------------------*/

            GetTokenResponse response = GetTokenResponse.newBuilder().setToken(token).setSignature(sigServerByteString).build();

            Timer timer = new Timer(30000, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {

                    if (getUsersMap().get(key) != null && getUsersMap().get(key).equals(token)) {
                        usersMap.replace(key, null);
                        writeToFile(getUsersMap(), USERS_FILE, MSG_USERS);
                        System.out.println("User token expired: " + key + ":" + token);
                    }
                }
            });
            timer.setRepeats(false);
            timer.start();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*----------------------------------------------------------------------------------------------------------------*/
    /*------------------------------------------------COMMANDS--------------------------------------------------------*/
    /*----------------------------------------------------------------------------------------------------------------*/
    @Override
    public synchronized void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
        System.out.println("Register Request Received: " + request);

        String key = request.getKey();
        /*--------------------------SIGNATURE AND HASH VALIDATE-----------------------------*/
        ByteString sigByteString = request.getSignature();

        byte[] sig = sigByteString.toByteArray();

        try {
            byte[] finalHash = Main.getHashFromObject(key);

            boolean valid = Main.validate(sig, key, finalHash); //key == userAlias
            sendArgumentError(!valid, responseObserver, MSG_ERROR_REGISTER_SIG);

        } catch (Exception e) {
            e.printStackTrace();
        }

        /*----------------------------------------------------------------------------------*/
        boolean valid = Main.hasCertificate(key);
        sendArgumentError(!valid, responseObserver, MSG_ERROR_CERTIFICATE);

        if (!getUsersMap().containsKey(key)) {
            getUsersMap().put(key, null);
            writeToFile(getUsersMap(), USERS_FILE, MSG_USERS);
            getParticularMap().put(key, new ArrayList<>());
            System.out.println("New user registered: " + key);
        } else
            System.out.println("User is already registered.");

        System.out.println("Users: " + getUsersMap());

        try {
            byte[] userHash = Main.getHashFromObject(key);
            byte[] sigGeneral = Main.getSignature(userHash, "server1");

            ByteString responseSigByteString = ByteString.copyFrom(sigGeneral);

            RegisterResponse response = RegisterResponse.newBuilder()
                    .setResult(key).setSignature(responseSigByteString).build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*--------------------------------------------------POSTS---------------------------------------------------------*/
    @Override
    public synchronized void post(PostRequest request, StreamObserver<PostResponse> responseObserver) {
        System.out.println("Post Request Received: " + request);

        Announcement post = request.getPost();
        String key = post.getKey();
        String message = post.getMessage();
        String token = request.getToken();
        int wts = request.getWts();

        boolean validRef = post.getRefList().isEmpty() || Collections.max(post.getRefList()) <= getPostId();
        sendArgumentError(!validRef, responseObserver, MSG_ERROR_INVALID_REF);

        boolean validLenght = message.length() <= 255;
        sendArgumentError(!validLenght, responseObserver, MSG_ERROR_POST_MSG_LEN);

        boolean validRegister = getUsersMap().containsKey(key);
        sendArgumentError(!validRegister, responseObserver, MSG_ERROR_NOT_REGISTERED);

        boolean validTimestamp = wts > timestamp;
        if(!validTimestamp){
            sendArgumentError(responseObserver, MSG_ERROR_INVALID_TIMESTAMP);
        }else{
            timestamp = wts;
        }

        /*--------------------------SIGNATURE AND HASH VALIDATE-----------------------------*/
        ByteString sigByteString = request.getSignature();

        byte[] signature = sigByteString.toByteArray();

        try {
            byte[] tokenHash = Main.getHashFromObject(token);
            byte[] postHash = Main.getHashFromObject(post.getKey());
            postHash = ArrayUtils.addAll(postHash, Main.getHashFromObject(post.getMessage()));
            byte[] wtsHash = Main.getHashFromObject(wts);
            byte[] finalHash = ArrayUtils.addAll(postHash, tokenHash);
            finalHash = ArrayUtils.addAll(finalHash, wtsHash);

            boolean valid = Main.validate(signature, key, finalHash); //key == userAlias
            sendArgumentError(!valid, responseObserver, MSG_ERROR_POST_SIG);

        } catch (Exception e) {
            e.printStackTrace();
        }


        if (getUsersMap().get(key) != null && getUsersMap().get(key).equals(token)) {
            getUsersMap().replace(key, null);
            writeToFile(getUsersMap(), USERS_FILE, MSG_USERS);

            System.out.println("User token expired: " + key + ":" + token);
        } else {
            sendArgumentError(responseObserver, MSG_ERROR_TOKEN_EXPIRED);
        }

        /*------------------------------------POST ID---------------------------------------*/
        postId++;
        writeToFile(getPostId(), POSTID_FILE, MSG_POSTID);

        /*----------------------BUILD ANNOUNCEMENTS OF THE POST-----------------------------*/
        Announcement.Builder postBuilder = post.toBuilder();
        postBuilder.setPostId(getPostId());
        postBuilder.setToken(token);
        postBuilder.setSignature(sigByteString);
        post = postBuilder.build();

        if (!getParticularMap().containsKey(key)) {
            ArrayList<Announcement> tmp = new ArrayList<Announcement>();
            tmp.add(post);
            getParticularMap().put(key, tmp);
        } else {
            ArrayList<Announcement> tmp = getParticularMap().get(key);
            tmp.add(post);
            getParticularMap().replace(key, tmp);
        }

        writeToFile(getParticularMap(), PARTICULAR_FILE, MSG_PARTICULAR);

        /*--------------------------SERVER SIGNATURE AND HASH-------------------------------*/
        try {
            byte[] userHash = Main.getHashFromObject(key);
            byte[] sigGeneral = Main.getSignature(userHash, "server1");

            ByteString responseSigByteString = ByteString.copyFrom(sigGeneral);

            PostResponse response = PostResponse.newBuilder()
                    .setResult(key).setSignature(responseSigByteString).build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public synchronized void postGeneral(PostGeneralRequest request, StreamObserver<PostGeneralResponse> responseObserver) {
        System.out.println("Post General Request Received: " + request);

        Announcement post = request.getPost();
        String key = post.getKey();
        String message = post.getMessage();
        String token = request.getToken();
        int wts = request.getWts();

        boolean validRef = post.getRefList().isEmpty() || Collections.max(post.getRefList()) <= getPostId();
        sendArgumentError(!validRef, responseObserver, MSG_ERROR_INVALID_REF);

        boolean validLenght = message.length() <= 255;
        sendArgumentError(!validLenght, responseObserver, MSG_ERROR_POST_MSG_LEN);

        boolean validRegister = getUsersMap().containsKey(key);
        sendArgumentError(!validRegister, responseObserver, MSG_ERROR_NOT_REGISTERED);

        boolean validTimestamp = wts > timestamp;
        if(!validTimestamp){
            sendArgumentError(responseObserver, MSG_ERROR_INVALID_TIMESTAMP);
        }else{
            timestamp = wts;
        }


        /*--------------------------SIGNATURE AND HASH VALIDATE-----------------------------*/
        ByteString sigByteString = request.getSignature();

        byte[] signature = sigByteString.toByteArray();
        try {
            byte[] postHash = Main.getHashFromObject(post.getKey());
            postHash = ArrayUtils.addAll(postHash, Main.getHashFromObject(post.getMessage()));
            byte[] tokenHash = Main.getHashFromObject(token);
            byte[] wtsHash = Main.getHashFromObject(wts);
            byte[] finalHash = ArrayUtils.addAll(postHash, tokenHash);
            finalHash = ArrayUtils.addAll(finalHash, wtsHash);

            boolean valid = Main.validate(signature, key, finalHash); //key == userAlias
            sendArgumentError(!valid, responseObserver, MSG_ERROR_POST_GENERAL_SIG);

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (getUsersMap().get(key) != null && getUsersMap().get(key).equals(token)) {

            getUsersMap().replace(key, null);
            writeToFile(getUsersMap(), USERS_FILE, MSG_USERS);

            System.out.println("User token expired: " + key + ":" + token);
        } else {
            sendArgumentError(responseObserver, MSG_ERROR_TOKEN_EXPIRED);
        }

        /*------------------------------------POST ID---------------------------------------*/
        postId++;
        writeToFile(getPostId(), POSTID_FILE, MSG_POSTID);

        /*----------------------BUILD ANNOUNCEMENTS OF THE POST-----------------------------*/
        Announcement.Builder postBuilder = post.toBuilder();
        postBuilder.setPostId(getPostId());
        postBuilder.setToken(token);
        postBuilder.setSignature(sigByteString);
        post = postBuilder.build();

        getGeneralMap().add(post);

        writeToFile(getGeneralMap(), GENERAL_FILE, MSG_GENERAL);

        System.out.println("General posts: " + getGeneralMap());

        /*--------------------------SERVER SIGNATURE AND HASH-------------------------------*/
        try {
            byte[] userHash = Main.getHashFromObject(key);
            byte[] sigGeneral = Main.getSignature(userHash, "server1");

            ByteString responseSigByteString = ByteString.copyFrom(sigGeneral);

            PostGeneralResponse response = PostGeneralResponse.newBuilder()
                    .setResult(key).setSignature(responseSigByteString).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*--------------------------------------------------READS---------------------------------------------------------*/
    @Override
    public synchronized void read(ReadRequest request, StreamObserver<ReadResponse> responseObserver) {
        System.out.println("Read Request Received: " + request);

        String userAlias = request.getKey();
        String key = request.getKeyToRead();
        int number = request.getNumber();
        String token = request.getToken();


        /*--------------------------SIGNATURE AND HASH VALIDATE-----------------------------*/
        ByteString sigByteString = request.getSignature();

        byte[] signature = sigByteString.toByteArray();

        try {
            byte[] tokenHash = Main.getHashFromObject(token);
            byte[] userAliasHash = Main.getHashFromObject(userAlias);
            byte[] keyHash = Main.getHashFromObject(key);
            byte[] numberHash = Main.getHashFromObject(number);
            byte[] finalHash = ArrayUtils.addAll(userAliasHash, keyHash);

            finalHash = ArrayUtils.addAll(finalHash, numberHash);
            finalHash = ArrayUtils.addAll(finalHash, tokenHash);

            boolean valid = Main.validate(signature, userAlias, finalHash); //key == userAlias
            sendArgumentError(!valid, responseObserver, MSG_ERROR_READ_SIG);

        } catch (Exception e) {
            e.printStackTrace();
        }

        boolean validRegister = getUsersMap().containsKey(key);

        if (!validRegister) {
            sendArgumentError(responseObserver, MSG_ERROR_NOT_REGISTERED);

        } else if (getUsersMap().get(userAlias).equals(token)) {
            getUsersMap().replace(userAlias, null);
            writeToFile(getUsersMap(), USERS_FILE, MSG_USERS);

            System.out.println("User token expired: " + userAlias + ":" + token);
        } else {
            sendArgumentError(responseObserver, MSG_ERROR_TOKEN_EXPIRED);
        }

        /*----------------------------------------------------------------------------------*/
        try {
            boolean validNumber = number >= 0;
            sendArgumentError(!validNumber, responseObserver, MSG_ERROR_READ_NUMBER);

            boolean validKey = getParticularMap().containsKey(key);

            if (!validKey) {
                getParticularMap().put(key, new ArrayList<Announcement>());
            }
            ArrayList<Announcement> tmp = getParticularMap().get(key);
            ArrayList<Announcement> result = new ArrayList<Announcement>();

            if (number > 0) {
                if (tmp.size() > 0) {
                    ListIterator<Announcement> listIter = tmp.listIterator(tmp.size());
                    for (int i = 0; i < number; i++) {
                        result.add(listIter.previous());
                    }
                }
            } else {
                Collections.reverse(tmp);
                result.addAll(tmp);
            }


            /*--------------------------SERVER SIGNATURE AND HASH-------------------------------*/

            byte[] hashGeneral = Main.getHashFromObject(result);
            byte[] sigGeneral = Main.getSignature(hashGeneral, "server1");

            ByteString responseSigByteString = ByteString.copyFrom(sigGeneral);

            ReadResponse response = ReadResponse.newBuilder().addAllResult(result).setSignature(responseSigByteString).setTs(timestamp).build();
            responseObserver.onNext(response);


            responseObserver.onCompleted();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void readGeneral(ReadGeneralRequest request, StreamObserver<ReadGeneralResponse> responseObserver) {
        System.out.println("Read General Request Received: " + request);

        String userAlias = request.getKey();
        int number = request.getNumber();
        String token = request.getToken();
        /*--------------------------SIGNATURE AND HASH VALIDATE-----------------------------*/
        ByteString sigByteString = request.getSignature();

        byte[] signature = sigByteString.toByteArray();

        try {
            byte[] tokenHash = Main.getHashFromObject(token);
            byte[] userAliasHash = Main.getHashFromObject(userAlias);
            byte[] numberHash = Main.getHashFromObject(number);
            byte[] finalHash = ArrayUtils.addAll(userAliasHash, numberHash);

            finalHash = ArrayUtils.addAll(finalHash, tokenHash);

            boolean valid = Main.validate(signature, userAlias, finalHash); //key == userAlias
            sendArgumentError(!valid, responseObserver, MSG_ERROR_READ_GENERAL_SIG);

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (getUsersMap().get(userAlias) != null && getUsersMap().get(userAlias).equals(token)) {

            getUsersMap().replace(userAlias, null);
            writeToFile(getUsersMap(), USERS_FILE, MSG_USERS);

            System.out.println("User token expired: " + userAlias + ":" + token);
        } else {
            sendArgumentError(responseObserver, MSG_ERROR_TOKEN_EXPIRED);
        }

        /*----------------------------------------------------------------------------------*/
        /*----------------------------------------------------------------------------------*/
        boolean validNumber = number >= 0;
        sendArgumentError(!validNumber, responseObserver, MSG_ERROR_READ_NUMBER);

        ArrayList<Announcement> general = getGeneralMap();
        ArrayList<Announcement> result = new ArrayList<Announcement>();

        if (number > 0) {
            ListIterator<Announcement> listIter = general.listIterator(general.size());
            for (int i = 0; i < number; i++) {
                result.add(listIter.previous());
            }

        } else {
            Collections.reverse(general);
            result.addAll(general);
        }


        /*--------------------------SERVER SIGNATURE AND HASH-------------------------------*/
        try {
            byte[] hashGeneral = Main.getHashFromObject(result);
            byte[] sigGeneral = Main.getSignature(hashGeneral, "server1");

            ByteString responseSigByteString = ByteString.copyFrom(sigGeneral);

            ReadGeneralResponse response = ReadGeneralResponse.newBuilder().addAllResult(result).setSignature(responseSigByteString).setTs(timestamp).build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void reset(ResetRequest request, StreamObserver<ResetResponse> responseObserver) {
        new File(USERS_FILE).delete();
        new File(PARTICULAR_FILE).delete();
        new File(GENERAL_FILE).delete();
        new File(POSTID_FILE).delete();
        initialize();
        timestamp = -1;
        ResetResponse response = ResetResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}
