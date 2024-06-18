import java.io.*;
import java.net.*;
import java.util.*;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class MathServer {
    private static final int PORT = 12345;
    private static Set<ClientHandler> clientHandlers = new HashSet<>();

    public static void main(String[] args) {
        System.out.println("Math server started...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandlers.add(clientHandler);
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
        private ScriptEngine engine;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            ScriptEngineManager manager = new ScriptEngineManager();
            this.engine = manager.getEngineByName("JavaScript");
        }

        @Override
        public void run() {
            try {
                writer = new PrintWriter(socket.getOutputStream(), true);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String message;
                while ((message = reader.readLine()) != null) {
                    if (message.startsWith("JOIN:")) {
                        userName = message.substring(5);
                        broadcast("SERVER: " + userName + " has joined the chat.");
                    } else if (message.startsWith("QUIT:")) {
                        broadcast("SERVER: " + userName + " has left the chat.");
                        break;
                    } else if (message.startsWith("MESSAGE:")) {
                        broadcast(message.substring(8));
                    } else if (message.startsWith("SOLVE:")) {
                        String expression = message.substring(6);
                        String result = solveExpression(expression);
                        writer.println("RESULT: " + expression + " = " + result);
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
            }
        }

        private String solveExpression(String expression) {
            try {
                Object result = engine.eval(expression);
                return result.toString();
            } catch (ScriptException ex) {
                return "Error evaluating expression: " + ex.getMessage();
            }
        }

        private void broadcast(String message) {
            for (ClientHandler handler : clientHandlers) {
                handler.writer.println(message);
            }
        }
    }
}
