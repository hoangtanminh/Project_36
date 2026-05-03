package com.auction.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {
    private static final int PORT = 8080;
    private static final ExecutorService pool = Executors.newFixedThreadPool(50);

    public static void main(String[] args) {
        System.out.println("[SERVER] Đang khởi động máy chủ...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[SERVER] Đang lắng nghe tại cổng " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[SERVER] Có Client mới: " + clientSocket.getInetAddress().getHostAddress());
                ClientHandler clientThread = new ClientHandler(clientSocket);
                pool.execute(clientThread);
            }
        } catch (IOException e) {
            System.err.println("[SERVER] Lỗi khởi động: " + e.getMessage());
        } finally {
            pool.shutdown();
        }
    }
}