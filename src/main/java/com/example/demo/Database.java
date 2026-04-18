package com.example.demo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class Database {

    public static void init() {
        try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:auction.db");

            Statement stmt = conn.createStatement();

            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS users (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "username TEXT," +
                            "password TEXT," +
                            "role TEXT)"
            );

            System.out.println("DB OK");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
