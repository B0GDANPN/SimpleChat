package org.example.Server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable {
    private final ServerSocket serverSocket;
    private static final Logger logger;

    static {
        logger = LogManager.getLogger(Server.class);
    }

    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
        logger.info("Server created with provided socket");
    }

    public Server() {
        try {
            serverSocket = new ServerSocket(7535);
            logger.info("Server created at port " + 7535);
        } catch (IOException e) {
            logger.fatal("IOException caused while Server creation");
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }

    public void stopAcceptingConnections() {
        try {
            serverSocket.close();
            logger.info("Server disabled to receive new connections");
        } catch (IOException e) {
            logger.fatal("Error while shutdown to receiving new connections");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        try {
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                logger.info("New connection accepted from " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
                Thread.ofVirtual().start(new UserHandler(socket));
            }
        } catch (IOException e) {
            stopAcceptingConnections();
            throw new RuntimeException(e);
        }
    }
}
