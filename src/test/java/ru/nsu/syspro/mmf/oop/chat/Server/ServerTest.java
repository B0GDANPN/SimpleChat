package ru.nsu.syspro.mmf.oop.chat.Server;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

class ServerTest {

    private Server server;
    private ServerSocket serverSocket;
    private Socket clientSocket;

    @BeforeAll
    static void setup() {
        Configurator.setRootLevel(Level.OFF);
    }

    @BeforeEach
    void setUp() throws IOException {
        serverSocket = mock(ServerSocket.class);
        clientSocket = mock(Socket.class);
        when(serverSocket.accept()).thenReturn(clientSocket);

        server = new Server(serverSocket);
    }

    @Test
    void testServerInitialization() throws IOException {
        assertNotNull(server);
        verify(serverSocket, never()).close();
    }

    @Test
    void testAcceptingClientConnection() throws IOException {
        new Thread(server).start();
        verify(serverSocket, timeout(1000)).accept();
    }

    @Test
    void testServerShutdown() throws IOException {
        server.stopAcceptingConnections();
        verify(serverSocket, times(1)).close();
    }

    @AfterEach
    void tearDown() throws IOException {
        serverSocket.close();
    }
}
