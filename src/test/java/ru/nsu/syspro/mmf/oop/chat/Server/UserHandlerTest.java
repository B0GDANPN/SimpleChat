package ru.nsu.syspro.mmf.oop.chat.Server;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.example.Server.UserHandler;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

class UserHandlerTest {

    private UserHandler userHandler;
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;

    @BeforeAll
    static void setup() {
        Configurator.setRootLevel(Level.OFF);
    }

    @BeforeEach
    void setUp() throws IOException {
        socket = mock(Socket.class);
        reader = mock(BufferedReader.class);
        writer = mock(BufferedWriter.class);

        when(socket.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        when(socket.getInetAddress()).thenReturn(InetAddress.getByName("localhost"));
        when(socket.getPort()).thenReturn(12345);

        userHandler = new UserHandler(socket);
        userHandler.setUsername("test_user");
        userHandler.setReader(reader);
        userHandler.setWriter(writer);
    }

    @Test
    void testUserHandlerInitialization() throws IOException {
        assertNotNull(userHandler);
        verify(socket, times(1)).getInputStream();
    }

    @Test
    void testHandlingClientMessages() throws IOException {
        when(reader.readLine()).thenReturn("Test Message", (String) null);
        when(socket.isConnected()).thenReturn(true, false);

        Thread userHandlerThread = new Thread(userHandler);
        userHandlerThread.start();

        try {
            userHandlerThread.join(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        verify(writer, times(1)).write("test_user: Test Message");
        verify(writer, times(1)).newLine();
        verify(writer, times(1)).flush();
    }

    @AfterEach
    void tearDown() throws IOException {
        socket.close();
    }
}
