package com.auction.server;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private Gson gson;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.gson = new Gson();
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            System.out.println("[CLIENT " + clientSocket.getPort() + "] Đã kết nối.");
            String clientMessage;

            while ((clientMessage = in.readLine()) != null) {
                System.out.println("Nhận từ Client: " + clientMessage);

                try {
                    Request request = gson.fromJson(clientMessage, Request.class);
                    Response response;

                    if (request != null && request.getAction() != null) {
                        switch (request.getAction()) {
                            case "LOGIN":
                                response = new Response("SUCCESS", "Đăng nhập thành công (giả lập)", null);
                                break;
                            case "GET_ITEMS":
                                response = new Response("SUCCESS", "Lấy danh sách vật phẩm thành công", null);
                                break;
                            default:
                                response = new Response("ERROR", "Hành động không hợp lệ", null);
                                break;
                        }
                    } else {
                        response = new Response("ERROR", "Sai định dạng Request", null);
                    }

                    String jsonResponse = gson.toJson(response);
                    out.println(jsonResponse);
                    System.out.println("Gửi trả Client: " + jsonResponse);

                } catch (Exception e) {
                    Response errRes = new Response("ERROR", "Dữ liệu không chuẩn JSON", null);
                    out.println(gson.toJson(errRes));
                }
            }
        } catch (IOException e) {
            System.err.println("Lỗi ngắt kết nối: " + e.getMessage());
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