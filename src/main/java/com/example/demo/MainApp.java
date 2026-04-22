package com.example.demo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Database.init();        //khởi tạo database

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/example/demo/login.fxml")  //trỏ đến file login.fxml
        );
        Scene scene = new Scene(loader.load(), 400, 350);   //load fxml tạo scene kích thước 400x350
        stage.setTitle("Hệ thống Đấu Giá");
        stage.setScene(scene); // gắn scene vào stage
        stage.show(); //hiển thị lên cửa sổ
    }

    public static void main(String[] args) {
        launch();
    }
}