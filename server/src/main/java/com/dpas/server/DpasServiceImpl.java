package com.dpas.server;

import com.dpas.Dpas.*;
import com.dpas.DpasServiceGrpc;
import com.dpas.crypto.Main;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
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
import java.util.List;
import java.util.Collections;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Comparator;

import static com.dpas.server.ServerDataStore.*;

public class DpasServiceImpl extends DpasServiceGrpc.DpasServiceImplBase {

    private HashMap<String, String> usersMap;
    private HashMap<String, ArrayList<Announcement>> particularMap;
    private ArrayList<Announcement> generalMap;
    private int postId;
    private int port;
    private int timestamp;
    private String timestampId;
    private final String USERS_FILE;
    private final String PARTICULAR_FILE;
    private final String GENERAL_FILE;
    private final String POSTID_FILE;


    int majority;
    int numOfFaults;
    int numOfServers;


    /*--------------------------------------------------FILES---------------------------------------------------------*/


    public DpasServiceImpl(int p, int nrFaults) {
        port = p;
        USERS_FILE = COMMON_USERS_FILE + port;
        PARTICULAR_FILE = COMMON_PARTICULAR_FILE + port;
        GENERAL_FILE = COMMON_GENERAL_FILE + port;
        POSTID_FILE = COMMON_POSTID_FILE + port;
        initialize();
        timestamp = -1;
        numOfFaults = nrFaults;
        numOfServers = 3 * numOfFaults + 1;
        majority = (int) Math.ceil((numOfServers + numOfFaults) / 2.0);
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

    public void validateBCB(StreamObserver<?> responseObserver, Announcement post, List<BroadcastResponse> bcb) {
        int counter = 0;
        try {
            for (BroadcastResponse res : bcb) {
                byte[] hash = Main.getHashFromObject(post);
                if(Main.validate(res.getSignature().toByteArray(), "server1", hash)){
                    counter++;
                }
            }
        } catch (Exception e){
            System.err.println(e.getMessage());
        }
        if(counter < majority){
            sendArgumentError(responseObserver, MSG_ERROR_BCB);
        }
    }

    /*---------------------------------------------------TOKENS-------------------------------------------------------*/
    @Override
    public synchronized void getToken(GetTokenRequest request, StreamObserver<GetTokenResponse> responseObserver) {
        System.out.println("\nGet Token Request Received:\nUser:" + request.getKey());

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
        System.out.println("\nToken atributed to user:" + token);


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
                        System.out.println("\nUser token expired: " + key + ":" + token);
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
        System.out.println("\nRegister Request Received:\n User: " + request.getKey());

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
            System.out.println("\nNew user registered: " + key);
        } else
            System.out.println("\nUser is already registered.");


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

        validateBCB(responseObserver, request.getPost(), request.getBcbList());

        Announcement post = request.getPost();
        String key = post.getKey();
        String message = post.getMessage();
        String token = request.getToken();
        int wts = request.getWts();

        System.out.println("\nPost Request Received:\n User: " + key+ "\nMessage: "
                + message +"\nTimestamp: "+ wts + "\nToken: " + request.getToken());

        boolean validRef = post.getRefList().isEmpty() || Collections.max(post.getRefList()) <= getPostId();
        sendArgumentError(!validRef, responseObserver, MSG_ERROR_INVALID_REF);

        boolean validLenght = message.length() <= 255;
        sendArgumentError(!validLenght, responseObserver, MSG_ERROR_POST_MSG_LEN);

        boolean validRegister = getUsersMap().containsKey(key);
        sendArgumentError(!validRegister, responseObserver, MSG_ERROR_NOT_REGISTERED);

        if(wts < timestamp || (wts == timestamp && key.compareTo(timestampId) >= 0)){
            sendArgumentError(responseObserver, MSG_ERROR_INVALID_TIMESTAMP);
        }

        timestamp++;
        timestampId = key;
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

            System.out.println("\nUser token expired: " + key + ":" + token);
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

        validateBCB(responseObserver, request.getPost(), request.getBcbList());

        Announcement post = request.getPost();
        String key = post.getKey();
        String message = post.getMessage();
        String token = request.getToken();
        int wts = request.getWts();
        System.out.println("\nPost General Request Received:\n User: " + key+ "\nMessage: "
                + message +"\nTimestamp: "+ wts + "\nToken: " + request.getToken());

        boolean validRef = post.getRefList().isEmpty() || Collections.max(post.getRefList()) <= getPostId();
        sendArgumentError(!validRef, responseObserver, MSG_ERROR_INVALID_REF);

        boolean validLenght = message.length() <= 255;
        sendArgumentError(!validLenght, responseObserver, MSG_ERROR_POST_MSG_LEN);

        boolean validRegister = getUsersMap().containsKey(key);
        sendArgumentError(!validRegister, responseObserver, MSG_ERROR_NOT_REGISTERED);

        if(wts < timestamp || (wts == timestamp && key.compareTo(timestampId) >= 0)){
            sendArgumentError(responseObserver, MSG_ERROR_INVALID_TIMESTAMP);
        }

        timestamp++;
        timestampId = key;

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

            System.out.println("\nUser token expired: " + key + ":" + token);
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
        System.out.println("\nRead Request Received:\n User: " + request.getKey() + "\nNumber: "
                + request.getNumber() + "\nToken: " + request.getToken());

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

            System.out.println("\nUser token expired: " + userAlias + ":" + token);
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
            byte[] hashTsId = Main.getHashFromObject(timestampId);
            byte[] hashTs = Main.getHashFromObject(timestamp);

            hashGeneral = ArrayUtils.addAll(hashGeneral, hashTsId);
            hashGeneral = ArrayUtils.addAll(hashGeneral, hashTs);
            byte[] sigGeneral = Main.getSignature(hashGeneral, "server1");

            ByteString responseSigByteString = ByteString.copyFrom(sigGeneral);

            ReadResponse response = ReadResponse.newBuilder().addAllResult(result).setSignature(responseSigByteString).setTs(timestamp).setTsId(timestampId).build();
            responseObserver.onNext(response);

            responseObserver.onCompleted();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void readGeneral(ReadGeneralRequest request, StreamObserver<ReadGeneralResponse> responseObserver) {
        System.out.println("\nRead General Request Received:\n User: " + request.getKey() + "\nNumber: "
                + request.getNumber() + "\nToken: " + request.getToken());

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
            byte[] hashTsId = Main.getHashFromObject(timestampId);
            byte[] hashTs = Main.getHashFromObject(timestamp);

            hashGeneral = ArrayUtils.addAll(hashGeneral, hashTsId);
            hashGeneral = ArrayUtils.addAll(hashGeneral, hashTs);
            byte[] sigGeneral = Main.getSignature(hashGeneral, "server1");

            ByteString responseSigByteString = ByteString.copyFrom(sigGeneral);

            ReadGeneralResponse response = ReadGeneralResponse.newBuilder().addAllResult(result).setSignature(responseSigByteString)
                    .setTs(timestamp).setTsId(timestampId).build();

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

    public synchronized void broadcast(BroadcastRequest bcbRequest, StreamObserver<BroadcastResponse> responseObserver) {
        System.out.println("\nBroadcast Request Received: " + bcbRequest);

        Announcement request = bcbRequest.getPost();

        try{
            byte[] hash = Main.getHashFromObject(request);
            byte[] sigBytes = Main.getSignature(hash, "server1");

            ByteString signature = ByteString.copyFrom(sigBytes);

            BroadcastResponse response = BroadcastResponse.newBuilder().setSignature(signature).setPost(request).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
