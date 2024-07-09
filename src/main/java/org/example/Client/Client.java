package org.example.Client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.Misc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Client extends Frame implements Runnable {
    private final TextArea textArea;
    private final TextField inputField;
    private volatile String username;

    private Socket connection;
    private BufferedReader reader;
    private BufferedWriter writer;
    private static final Logger logger;

    private final TrayIcon trayIcon;
    private volatile boolean exiting;

    static {
        logger = LogManager.getLogger();
    }

    private class UsernameForm extends Frame implements ActionListener {
        Label label;
        TextField textField;
        Button submitButton;

        public UsernameForm() {
            logger.info("Creating form for username asking");
            setLayout(new FlowLayout());
            setTitle("Username");
            setSize(300, 100);

            label = new Label("Enter your username:");
            add(label);

            textField = new TextField(20);
            add(textField);

            submitButton = new Button("set");
            submitButton.addActionListener(this);
            add(submitButton);

            addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent we) {
                    System.exit(0);
                }
            });

            try {
                connection = new Socket("localhost", 7535);
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));

                String buffer = reader.readLine();
            } catch (IOException e) {
                Misc.close(connection, reader, writer);
                throw new UncheckedIOException(e);
            }

            setVisible(true);
            logger.info("Waiting for username input");
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            String uname = textField.getText();

                if (uname.contains(" ")) {
                    logger.info("Username with spaces entered, asking again");
                    textField.setText("Username cannot contain spaces, select other please");
                } else {
                    logger.info("Username " + uname + " entered");
                    username = uname;
                    setVisible(false);
                    dispose();
                    logger.info("Closing username input form");
                }
        }
    }

    public Client() {
        logger.info("Creating new client");
        setTitle("Chat");
        setSize(400, 300);
        setLayout(new BorderLayout());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                SystemTray.getSystemTray().remove(trayIcon);
                exiting = true;
                dispose();
            }
        });

        textArea = new TextArea();
        textArea.setEditable(false);
        add(textArea, BorderLayout.CENTER);

        inputField = new TextField();
        inputField.addActionListener(e -> {
            String input = inputField.getText();
            logger.info("Received message from ui, sending to server: " + input);
            inputField.setText("");//оправка сообщения от пользователя

            try {
                writer.write(input);
                writer.newLine();
                writer.flush();
            } catch (IOException ex) {
                logger.error("Error while trying to send message");
                throw new RuntimeException(ex);
            }
            logger.info("Message sent");
        });

        logger.info("Adding icon to tray");
        Image image = Toolkit.getDefaultToolkit().getImage("java.awt.Toolkit.getDefaultToolkit()");
        trayIcon = new TrayIcon(image, "chat icon");
        trayIcon.setImageAutoSize(true);
        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException e) {
            logger.fatal("Error while adding icon to tray");
            throw new RuntimeException(e);
        }

        add(inputField, BorderLayout.SOUTH);
    }

    private void listen() {
        Thread.ofVirtual().start(
                () -> {
                    while (connection.isConnected() && !exiting) {
                        try {//присылает SYSTEM Coneected //сообщение// .. если не закрыт
                            String message = reader.readLine();
                            if (message == null) {
                                connection.close();
                                break;
                            }
                            logger.info("Received message " + message + ", appending it to ui");

                            int messageBeginIdx = 0;
                            while (message.charAt(messageBeginIdx) != ' ') {
                                messageBeginIdx++;
                            }
                            if (message.charAt(++messageBeginIdx) == '@') {
                                int beg = ++messageBeginIdx;
                                logger.info("Processing message with notification");
                                while (message.charAt(messageBeginIdx) != ' ') {
                                    messageBeginIdx++;
                                }
                                String messageTo = message.substring(beg, messageBeginIdx);
                                if (messageTo.equals(username)) {
                                    logger.info("Message with notification for this client, notifying");
                                    trayIcon.displayMessage("Notification", "You was received a message", TrayIcon.MessageType.INFO);
                                }
                            }

                            SwingUtilities.invokeLater(() -> textArea.append(message + "\n"));
                        } catch (IOException e) {
                            logger.error("Error caused while receiving message");
                            Misc.close(connection, reader, writer);
                            throw new RuntimeException(e);
                        }
                    }
                }
        );
    }

    @Override
    public void run() {
        new UsernameForm();
        while (username == null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        setVisible(true);

        try {
            writer.write(username);
            writer.newLine();
            writer.flush();

            listen();
        } catch (IOException e) {
            logger.error("Error while message write to socket");
            Misc.close(connection, reader, writer);
            throw new UncheckedIOException(e);
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }
}
