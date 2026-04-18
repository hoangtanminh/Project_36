package com.auction.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AuctionFxApp extends Application {
    private Stage stage;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        stage.setTitle("Auction App");
        showLoginScreen();
        stage.show();
    }

    private void showLoginScreen() {
        Label title = new Label("Login");
        title.setStyle("-fx-font-size: 22; -fx-font-weight: bold;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        Button loginButton = new Button("Dang nhap");
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(event -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Vui long nhap day du username/password");
                alert.setHeaderText("Thong tin chua hop le");
                alert.showAndWait();
                return;
            }

            showListScreen(username);
        });

        VBox loginLayout = new VBox(12, title, usernameField, passwordField, loginButton);
        loginLayout.setPadding(new Insets(24));
        loginLayout.setAlignment(Pos.CENTER);
        loginLayout.setPrefWidth(360);

        stage.setScene(new Scene(loginLayout, 420, 300));
    }

    private void showListScreen(String username) {
        Label title = new Label("Danh sach dau gia");
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold;");

        Label welcome = new Label("Xin chao, " + username);

        ListView<String> itemList = new ListView<>();
        itemList.getItems().addAll(
                "iPhone 16 - Gia hien tai: 25,000,000",
                "Tranh son dau - Gia hien tai: 12,500,000",
                "Xe may dien - Gia hien tai: 18,000,000"
        );
        VBox.setVgrow(itemList, Priority.ALWAYS);

        Button logoutButton = new Button("Dang xuat");
        logoutButton.setOnAction(event -> showLoginScreen());

        HBox footer = new HBox(logoutButton);
        footer.setAlignment(Pos.CENTER_RIGHT);

        VBox listLayout = new VBox(12, title, welcome, itemList, footer);
        listLayout.setPadding(new Insets(20));
        listLayout.setPrefSize(640, 420);

        stage.setScene(new Scene(listLayout));
    }
}
