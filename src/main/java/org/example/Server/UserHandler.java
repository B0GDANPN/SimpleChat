package org.example.Server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.Misc;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UserHandler implements Runnable {
    private final Socket connection;
    private static final List<UserHandler> users;
    protected static Logger logger;
    private BufferedReader reader;
    private BufferedWriter writer;
    private String username;

    static {
        users = new ArrayList<>();
        logger = LogManager.getLogger(UserHandler.class);
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public UserHandler(Socket connection) {
        this.connection = connection;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            writer.write(String.valueOf(users.size()));
            writer.newLine();
            writer.flush();
            for (UserHandler userHandler : users) {
                writer.write(userHandler.username);
                writer.newLine();
                writer.flush();
            }
            username = reader.readLine();
        } catch (IOException e) {
            logger.error("Error while trying to add client: " + Arrays.toString(e.getStackTrace()));
            Misc.close(writer, reader, connection);
            return;
        }
        users.add(this);

        broadcastSystemMessage(username + " connected");
        logger.info("New client created with username " + username + ". Connection info: " + getClientAddress());
    }

    private void broadcastSystemMessage(String message) {
        broadcastMessage("SYSTEM: " + message, true);
    }

    private void sendMessage(String message, String username) {
        logger.info("Sending message " + message + " to user " + username);
        for (UserHandler user : users) {
            if (user.username.equals(username)) {
                try {
                    user.writer.write(message);
                    user.writer.newLine();
                    user.writer.flush();
                } catch (IOException e) {
                    logger.error("Error while sending message to user " + user.username + " from " + username);
                    Misc.close(connection, reader, writer);
                    return;
                }
                break;
            }
        }
        logger.trace("Sending message to user " + username + " finished");
    }

    private void broadcastMessage(String message, boolean includingSender) {
        logger.info("Broadcasting message: " + message);
        for (UserHandler user : users) {
            if (includingSender || !user.username.equals(username)) {
                try {
                    user.writer.write(message);
                    user.writer.newLine();
                    user.writer.flush();
                } catch (IOException e) {
                    logger.error("Error while sending message to user " + user.username + " from " + username);
                    Misc.close(connection, reader, writer);
                    return;
                }
            }
        }
        logger.trace("Broadcast message to all users finished");
    }

    private String getClientAddress() {
        return connection.getInetAddress().getHostName() + ":" + connection.getPort();
    }

    @Override
    public void run() {
        while (connection.isConnected()) {
            try {
                String message = reader.readLine();
                if (message == null) {
                    connection.close();
                    break;
                }
                logger.info("Received message " + message + " from " + username + ", broadcasting");
                if (!message.isEmpty()) {
                    if (message.charAt(0) == '@') {
                        int endIdx = 1;
                        while (message.charAt(endIdx) != ' ') {
                            endIdx++;
                        }
                        String receiverName = message.substring(1, endIdx);
                        sendMessage(username + ": " + message, username);
                        sendMessage(username + ": " + message, receiverName);
                    } else {
                        switch (message) {
                            case "/list" -> {
                                sendMessage("Now connected " + users.size() + " users:", username);
                                for (int i = 0; i < users.size(); i++) {
                                    sendMessage("    " + users.get(i).username, username);
                                }
                            }
                            default -> broadcastMessage(username + ": " + message, true);
                        }
                    }
                }
            } catch (IOException e) {
                logger.error("Error caused while receiving message from " + getClientAddress());
                Misc.close(connection, reader, writer);
                break;
            }
        }
        disconnect();
    }

    public void disconnect() {
        users.remove(this);
        broadcastSystemMessage(username + " disconnected");
        logger.info(username + " disconnected from server");
    }

    public void setReader(BufferedReader reader) {
        this.reader = reader;
    }

    public void setWriter(BufferedWriter writer) {
        this.writer = writer;
    }
}
