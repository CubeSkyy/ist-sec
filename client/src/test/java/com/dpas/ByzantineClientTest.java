package com.dpas;


import com.dpas.client.ClientAPI;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.dpas.ClientDataStore.*;
import static com.dpas.server.ServerDataStore.MSG_ERROR_BCB;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class ByzantineClientTest extends RollbackTestAbstractClass {

    @Test
    public void changeMessageBeforeBCB() throws Exception {
        changeMessageBCBAPI client = new changeMessageBCBAPI(numOfServers, numOfFaults);

        client.receive(stubs, "register|" + CLIENT_TEST_USER);
        ArrayList<Dpas.PostResponse> res = (ArrayList) client.receive(stubs, "post|" + CLIENT_TEST_USER + "|" + CLIENT_TEST_MSG);

        assertTrue(StringUtils.countMatches(errContent.toString(), MSG_ERROR_BCB) >= client.majority);
        assertEquals(0, res.size());

    }

    private class changeMessageBCBAPI extends ClientAPI {
        AtomicInteger counter = new AtomicInteger(0);

        public changeMessageBCBAPI(int numOfServers, int numOfFaults) {
            super(numOfServers, numOfFaults);
        }

        @Override
        public Dpas.Announcement buildAnnouncement(String[] command) {
            String userAlias = command[1];
            List<Integer> referral = new ArrayList<Integer>();
            if (command.length > 2) {
                int i = 3;
                while (i < command.length) {
                    referral.add(Integer.parseInt(command[i]));
                    i += 1;
                }
            }
            String message;
            if (counter.getAndIncrement() % 2 == 1)
                message = CLIENT_TEST_MSG2;
            else
                message = command[2];


            Dpas.Announcement.Builder post = Dpas.Announcement.newBuilder().setKey(userAlias).setMessage(message).setWts(wts).setGeneral(command[0].equals("postGeneral"));
            post.addAllRef(referral);
            return post.build();
        }
    }

}




