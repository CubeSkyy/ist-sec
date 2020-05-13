package com.dpas;

import com.dpas.Dpas.ReadResponse;
import com.dpas.server.ServerDataStore;
import io.grpc.StatusRuntimeException;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;

import static com.dpas.ClientDataStore.*;
import static com.dpas.server.ServerDataStore.MSG_ERROR_NOT_REGISTERED;
import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class ReadClientTest extends RollbackTestAbstractClass {

    @Test
    public void readValid() throws Exception {

        client.receive(stubs, "register|" + CLIENT_TEST_USER);
        ArrayList<Dpas.PostResponse> res = (ArrayList) client.receive(stubs, "post|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG);
        for (Dpas.PostResponse ele : res) {
            assertEquals(ele.getResult(), CLIENT_TEST_USER);
        }
        ArrayList<ReadResponse> res2 = (ArrayList) client.receive(stubs, "read|" + CLIENT_TEST_USER +
                "|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG_NUMBER);


        for (ReadResponse ele : res2) {
            assertEquals(CLIENT_TEST_MSG_NUMBER, Integer.toString(ele.getResultCount()));
            assertEquals(CLIENT_TEST_USER, ele.getResult(0).getKey());
            assertEquals(CLIENT_TEST_MSG, ele.getResult(0).getMessage());
            assertEquals(0, ele.getResult(0).getRefCount());
            assertEquals(1, ele.getResult(0).getPostId());
        }

    }

    @Test
    public void readNonRegistered() throws Exception {

        ArrayList<Dpas.RegisterResponse> res = (ArrayList) client.receive(stubs, "read|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_USER + "|" + 0);
        assertTrue(StringUtils.countMatches(errContent.toString(), MSG_ERROR_NOT_REGISTERED) >= client.majority);
        assertTrue(res.size() == 0);
    }


}
