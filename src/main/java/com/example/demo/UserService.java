package com.example.demo;

import java.sql.*;

public class UserService {

    private static final String URL = "jdbc:sqlite:auction.db";

    //  1. Thêm user (SIGN UP)
    public static void insertUser(String username, String password) {
        try {
            Connection conn = DriverManager.getConnection(URL);

            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users(username, password, role) VALUES (?, ?, ?)"
            );

            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, "BIDDER");

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //  2. Check username đã tồn tại chưa
    public static boolean exists(String username) {
        try {
            Connection conn = DriverManager.getConnection(URL);

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM users WHERE username=?"
            );

            ps.setString(1, username);

            ResultSet rs = ps.executeQuery();

            return rs.next(); // có dữ liệu = true

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    //  3. Login
    public static boolean login(String username, String password) {
        try {
            Connection conn = DriverManager.getConnection(URL);

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM users WHERE username=? AND password=?"
            );

            ps.setString(1, username);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            return rs.next(); // có user = login OK

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}