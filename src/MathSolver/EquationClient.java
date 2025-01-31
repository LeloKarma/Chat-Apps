package MathSolver;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class EquationClient {
    public static void main(String[] args) {
        String hostname = "192.168.100.6";  // Replace with server's IP address
        int port = 12345;

        try (Socket socket = new Socket(hostname, port);
             OutputStream output = socket.getOutputStream();
             InputStream input = socket.getInputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(output);
             ObjectInputStream objectInputStream = new ObjectInputStream(input)) {

        
            Scanner scanner = new Scanner(System.in);

            // Read the number of equations (and variables)
            System.out.print("Enter the number of equations: ");
            int n = scanner.nextInt();

            double[][] coefficients = new double[n][n];
            double[] constants = new double[n];

            // Read coefficients of the equations
            for (int i = 0; i < n; i++) {
                System.out.println("Enter coefficients for equation " + (i + 1) + ":");
                for (int j = 0; j < n; j++) {
                    coefficients[i][j] = scanner.nextDouble();
                }
                System.out.print("Enter the constant term for equation " + (i + 1) + ": ");
                constants[i] = scanner.nextDouble();
            }

            // Send the coefficients and constants to the server
            objectOutputStream.writeObject(coefficients);
            objectOutputStream.writeObject(constants);

            // Receive the results from the server
            double[] results = (double[]) objectInputStream.readObject();

            // Display the solutions
            System.out.println("Solutions:");
            for (int i = 0; i < results.length; i++) {
                System.out.println("Variable " + (i + 1) + " = " + results[i]);
            }

        } catch (IOException | ClassNotFoundException ex) {
            System.out.println("Client exception: " + ex.getMessage());
            ex.printStackTrace();}}
}
