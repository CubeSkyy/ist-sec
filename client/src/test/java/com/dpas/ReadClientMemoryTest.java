//package com.dpas;
//
//import com.dpas.client.ClientAPI;
//import com.dpas.crypto.Main;
//import com.google.protobuf.ByteString;
//import org.apache.commons.lang3.ArrayUtils;
//import org.apache.commons.lang3.StringUtils;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.junit.runners.JUnit4;
//
//import java.util.ArrayList;
//
//import static com.dpas.ClientDataStore.*;
//import static org.junit.Assert.*;
//
//
//@RunWith(JUnit4.class)
//public class ReadClientMemoryTest extends RollbackTestAbstractClass {
//
//    @Test
//    public void readAfterChange() throws Exception {
//        changeMessageReceived client = new changeMessageReceived(numOfServers, numOfFaults);
//        client.receive(stubs, "register|" + CLIENT_TEST_USER);
//        client.receive(stubs, "post|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG);
//        ArrayList<Dpas.RegisterResponse> res = (ArrayList) client.receive(stubs, "read|" + CLIENT_TEST_USER +"|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG_NUMBER);
//
//        assertTrue(StringUtils.countMatches(errContent.toString(), "Invalid read signature. Read was corrupted") >= client.majority);
//        assertNull(res);
//    }
//
//
//    private class changeMessageReceived extends ClientAPI {
//        public changeMessageReceived(int numOfServers, int numOfFaults) {
//            super(numOfServers, numOfFaults);
//        }
//
//        @Override
//        public Dpas.ReadResponse read(DpasServiceGrpc.DpasServiceBlockingStub stub, String[] command, ArrayList<Dpas.BroadcastResponse> bcb) throws Exception {
//            String userAlias = command[1];
//            String key = command[2];
//            int number = Integer.parseInt(command[3]);
//            /*-------------------------------GET TOKEN VALIDATION-------------------------------*/
//            Dpas.GetTokenResponse responseGetToken = getClientToken(stub, userAlias);
//
//            boolean tokenVerify = validateToken(responseGetToken);
//
//            if (!tokenVerify) {
//                System.err.println("Invalid signature and/or hash. GetToken request corrupted.");
//                return null;
//            }
//            /*----------------------------------------------------------------------------------*/
//            String token = responseGetToken.getToken();
//            byte[] keyHash = Main.getHashFromObject(key);
//            byte[] numberHash = Main.getHashFromObject(number);
//            byte[] userAliasHash = Main.getHashFromObject(userAlias);
//            byte[] tokenHash = Main.getHashFromObject(token);
//
//            byte[] hash = ArrayUtils.addAll(userAliasHash, keyHash); //userAlias + key + number + token
//            hash = ArrayUtils.addAll(hash, numberHash);
//            hash = ArrayUtils.addAll(hash, tokenHash);
//            byte[] signature = Main.getSignature(hash, userAlias);
//
//            Dpas.ReadRequest requestRead = Dpas.ReadRequest.newBuilder().setNumber(number).setKey(userAlias)
//                    .setKeyToRead(key).setSignature(ByteString.copyFrom(signature)).setToken(token).build();
//
//            Dpas.ReadResponse responseRead = stub.read(requestRead);
//
//            /*---------------------------------SERVER VALIDATION--------------------------------*/
//            ByteString sigServerByteString = responseRead.getSignature();
//            ArrayList<Dpas.Announcement> result = new ArrayList<Dpas.Announcement>(responseRead.getResultList());
//            byte[] resultHash = Main.getHashFromObject(result);
//            byte[] hashTsId = Main.getHashFromObject(responseRead.getTsId());
//            byte[] hashTs = Main.getHashFromObject(responseRead.getTs());
//
//            resultHash = ArrayUtils.addAll(resultHash, hashTsId);
//            resultHash = ArrayUtils.addAll(resultHash, hashTs);
//            boolean validResponse = validateServerResponse(sigServerByteString, resultHash);
//            if (!validResponse) {
//                System.err.println("Invalid signature and/or hash. Read Response corrupted.");
//                return null;
//            }
//
//            responseRead = responseRead.toBuilder().setResult(0, responseRead.toBuilder().getResult(0).toBuilder().setMessage("Changed Message").build()).build();
//
//            return responseRead;
//        }
//
//    }
//}