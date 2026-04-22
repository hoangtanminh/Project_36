package com.auction.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            out.println("Chào mừng bạn đến với Server!");
            String clientMessage;

            while ((clientMessage = in.readLine()) != null) {
                System.out.println("[CLIENT " + clientSocket.getPort() + "] Nói: " + clientMessage);
                if (clientMessage.equalsIgnoreCase("QUIT")) {
                    out.println("Tạm biệt!");
                    break;
                }
                out.println("Server đã nhận: " + clientMessage);
            }
        } catch (IOException e) {
            System.err.println("Lỗi Client: " + e.getMessage());
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (clientSocket != null) clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
