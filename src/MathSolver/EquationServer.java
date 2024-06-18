package MathSolver;

import java.io.*;
import java.net.*;

public class EquationServer {
    public static void main(String[] args) {
        String ipAddress = "192.168.8.102";
        int port = 12345;

        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.bind(new InetSocketAddress(ipAddress, port));
            System.out.println("Server is listening on IP " + ipAddress + " and port " + port);

            while (true) {
                new EquationSolver(serverSocket.accept()).start();
            }

        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}

class EquationSolver extends Thread {
    private Socket socket;

    public EquationSolver(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try (InputStream input = socket.getInputStream();
             OutputStream output = socket.getOutputStream();
             ObjectInputStream objectInputStream = new ObjectInputStream(input);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(output)) {

            System.out.println("Connected to client at " + socket.getRemoteSocketAddress());

            double[][] coefficients = (double[][]) objectInputStream.readObject();
            double[] constants = (double[]) objectInputStream.readObject();

            System.out.println("Received coefficients:");
            printMatrix(coefficients);
            System.out.println("Received constants:");
            printArray(constants);

            double[] results = solve(coefficients, constants);

            objectOutputStream.writeObject(results);
            System.out.println("Results sent to client:");
            printArray(results);

        } catch (IOException | ClassNotFoundException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                socket.close();
                System.out.println("Connection with client closed.");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private double[] solve(double[][] coefficients, double[] constants) {
        int n = constants.length;
        double[] x = new double[n];

        System.out.println("Starting Gaussian elimination.");

        // Perform Gaussian elimination
        for (int k = 0; k < n; k++) {
            // Find the pivot row
            int max = k;
            for (int i = k + 1; i < n; i++) {
                if (Math.abs(coefficients[i][k]) > Math.abs(coefficients[max][k])) {
                    max = i;
                }
            }

            // Swap the pivot row with the current row
            double[] temp = coefficients[k];
            coefficients[k] = coefficients[max];
            coefficients[max] = temp;

            double t = constants[k];
            constants[k] = constants[max];
            constants[max] = t;

            System.out.println("Pivot at row " + k + " with max row " + max);
            printMatrix(coefficients);
            printArray(constants);

            // Make all rows below this one 0 in the current column
            for (int i = k + 1; i < n; i++) {
                double factor = coefficients[i][k] / coefficients[k][k];
                constants[i] -= factor * constants[k];
                for (int j = k; j < n; j++) {
                    coefficients[i][j] -= factor * coefficients[k][j];
                }
            }
            System.out.println("After elimination step " + k + ":");
            printMatrix(coefficients);
            printArray(constants);
        }

        // Solve equation Ax=b for an upper triangular matrix A
        for (int i = n - 1; i >= 0; i--) {
            double sum = 0.0;
            for (int j = i + 1; j < n; j++) {
                sum += coefficients[i][j] * x[j];
            }
            x[i] = (constants[i] - sum) / coefficients[i][i];
        }

        System.out.println("Gaussian elimination completed.");
        return x;
    }

    private void printMatrix(double[][] matrix) {
        for (double[] row : matrix) {
            for (double value : row) {
                System.out.print(value + " ");
            }
            System.out.println();
        }
    }

    private void printArray(double[] array) {
        for (double value : array) {
            System.out.print(value + " ");
        }
        System.out.println();
    }
}
