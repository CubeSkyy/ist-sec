package com.dpas;

import com.dpas.Dpas.ReadResponse;
import com.dpas.server.ServerDataStore;
import io.grpc.StatusRuntimeException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.dpas.ClientDataStore.*;
import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class ReadClientTest extends RollbackTestAbstractClass {
//
//    @Test
//    public void readValid() throws Exception {
//        client.receive(blockingStub, "register|" + CLIENT_TEST_USER);
//        boolean postResponse = client.post(blockingStub, client.getCommand("post|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG));
//        ReadResponse readResponse = client.read(blockingStub, client.getCommand("read|" + CLIENT_TEST_USER +
//                "|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG_NUMBER));
//
//        assertTrue(postResponse);
//        assertEquals(CLIENT_TEST_MSG_NUMBER, Integer.toString(readResponse.getResultCount()));
//        assertEquals(CLIENT_TEST_USER, readResponse.getResult(0).getKey());
//        assertEquals(CLIENT_TEST_MSG, readResponse.getResult(0).getMessage());
//        assertEquals(0, readResponse.getResult(0).getRefCount());
//        assertEquals(1, readResponse.getResult(0).getPostId());
//    }
//
//    @Test
//    public void readNonRegistered() throws Exception {
//        Throwable exception = assertThrows(StatusRuntimeException.class, () -> {
//            client.read(blockingStub, client.getCommand("read|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_USER + "|" + 0));
//        });
//        assertEquals(ServerDataStore.MSG_ERROR_NOT_REGISTERED, ((StatusRuntimeException) exception).getStatus().getDescription());
//
//    }


}
