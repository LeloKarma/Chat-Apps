import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChatServer {
    private static final int PORT = 12345;
    private static Set<ClientHandler> clientHandlers = Collections.synchronizedSet(new HashSet<>());
    private static Set<String> userNames = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) {
        System.out.println("Chat server started...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            // Start a thread to read console input for the server to chat
            new Thread(new ServerConsoleHandler()).start();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                new Thread(clientHandler).start();
            }
        } catch (IOException ex) {
            System.out.println("Server error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter writer;
        private BufferedReader reader;
        private String userName;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                writer = new PrintWriter(socket.getOutputStream(), true);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String message;
                while ((message = reader.readLine()) != null) {
                    if (message.startsWith("JOIN:")) {
                        userName = message.substring(5).trim();
                        if (userName.isEmpty() || userNames.contains(userName)) {
                            writer.println("ERROR: Invalid or duplicate username.");
                        } else {
                            userNames.add(userName);
                            clientHandlers.add(this);
                            broadcast("SERVER: " + userName + " has joined the chat.");
                            logMessage("SERVER: " + userName + " has joined the chat.");
                            sendUserList();
                        }
                    } else if (message.startsWith("QUIT:")) {
                        handleQuitCommand();
                        break;
                    } else if (message.startsWith("MESSAGE:")) {
                        String chatMessage = userName + ": " + message.substring(8);
                        broadcast(chatMessage);
                        logMessage(chatMessage);
                    } else if (message.startsWith("KICK:")) {
                        String[] parts = message.split(":");
                        if (parts.length >= 2) {
                            String userToKick = parts[1].trim();
                            handleKickCommand(userToKick);
                        } else {
                            writer.println("ERROR: Invalid kick command format.");
                        }
                    }
                }
            } catch (IOException ex) {
                System.out.println("Client error: " + ex.getMessage());
                ex.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                clientHandlers.remove(this);
                if (userName != null) {
                    userNames.remove(userName);
                    broadcast("SERVER: " + userName + " has left the chat.");
                    logMessage("SERVER: " + userName + " has left the chat.");
                }
            }
        }

        private void broadcast(String message) {
            synchronized (clientHandlers) {
                for (ClientHandler handler : clientHandlers) {
                    handler.writer.println(message);
                }
            }
        }

        private void sendUserList() {
            writer.println("SERVER: Current users: " + String.join(", ", userNames));
        }

        private void logMessage(String message) {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            System.out.println(timestamp + " " + message);
        }

        private void handleQuitCommand() {
            String quitMessage = "SERVER: " + userName + " has left the chat.";
            broadcast(quitMessage);
            logMessage(quitMessage);
        }

        private void handleKickCommand(String userToKick) throws IOException {
            synchronized (clientHandlers) {
                for (ClientHandler handler : clientHandlers) {
                    if (handler.userName.equals(userToKick)) {
                        handler.writer.println("SERVER: You have been kicked from the chat.");
                        handler.socket.close();
                        clientHandlers.remove(handler);
                        userNames.remove(handler.userName);
                        broadcast("SERVER: " + handler.userName + " has been kicked from the chat.");
                        logMessage("SERVER: " + handler.userName + " has been kicked from the chat.");
                        break;
                    }
                }
            }
        }
    }

    static class ServerConsoleHandler implements Runnable {
        @Override
        public void run() {
            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    String message = scanner.nextLine();
                    if (!message.isEmpty()) {
                        if (message.equalsIgnoreCase("list")) {
                            logMessage("SERVER: Current users: " + String.join(", ", userNames));
                        } else {
                            String serverMessage = "SERVER: " + message;
                            broadcastToAll(serverMessage);
                            logMessage(serverMessage);
                        }
                    }
                }
            } catch (Exception ex) {
                System.out.println("Console input error: " + ex.getMessage());
                ex.printStackTrace();
            }
        }

        private void broadcastToAll(String message) {
            synchronized (clientHandlers) {
                for (ClientHandler handler : clientHandlers) {
                    handler.writer.println(message);
                }
            }
        }

        private void logMessage(String message) {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            System.out.println(timestamp + " " + message);
        }
    }
}
