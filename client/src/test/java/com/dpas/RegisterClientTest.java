package com.dpas;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import com.dpas.HelloWorld.RegisterResponse;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class RegisterClientTest extends RollbackTestAbstractClass {

    @Test
    public void registeredUserTest() throws Exception {
        RegisterResponse response = client.register(blockingStub, client.getCommand("register|" + DataStore.TEST_USER));

        assertTrue(response.getResult());
    }

    @Test
    public void nonRegisteredUserTest() throws Exception {
        Throwable exception = assertThrows(io.grpc.StatusRuntimeException.class, () -> {
            RegisterResponse response = client.register(blockingStub, client.getCommand("register|" + DataStore.WRONG_USER));
        });
        assertEquals("INVALID_ARGUMENT: User is not registered in keystore.", exception.getMessage());
    }

    //TODO: Tests for authentication and integrity when its implemented in client/server

}
