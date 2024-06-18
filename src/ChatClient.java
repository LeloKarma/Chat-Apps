import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient {
    private static final String SERVER_IP = "172.20.10.6";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            Scanner scanner = new Scanner(System.in);
            String userName;

            System.out.print("Enter your name: ");
            userName = scanner.nextLine();
            writer.println("JOIN:" + userName);

            new Thread(new ReaderHandler(reader)).start();

            String message;
            while (true) {
                System.out.print("> ");
                message = scanner.nextLine();

                if (message.equalsIgnoreCase("quit")) {
                    writer.println("QUIT:" + userName);
                    break;
                } else {
                    writer.println("MESSAGE:" + userName + ": " + message);
                }
            }

        } catch (IOException ex) {
            System.out.println("Client error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    static class ReaderHandler implements Runnable {
        private BufferedReader reader;

        public ReaderHandler(BufferedReader reader) {
            this.reader = reader;
        }

        @Override
        public void run() {
            String response;
            try {
                while ((response = reader.readLine()) != null) {
                    System.out.println(response);
                }
            } catch (IOException ex) {
                System.out.println("Error reading from server: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }
}