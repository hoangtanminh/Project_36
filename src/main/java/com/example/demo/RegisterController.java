package com.example.demo;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisible;
    @FXML private Label msgLabel;
    @FXML private Button backBtn;
    @FXML private Button toggleRegPass;

    private boolean passShown = false;

    @FXML
    private void handleRegister() throws Exception {
        String user = usernameField.getText();
        String pass = passShown ? passwordVisible.getText() : passwordField.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            msgLabel.setStyle("-fx-text-fill: red;");
            msgLabel.setText("Không được để trống!");
            return;
        }

        if (UserService.exists(user)) {
            msgLabel.setStyle("-fx-text-fill: red;");
            msgLabel.setText("Username đã tồn tại!");
            return;
        }

        UserService.insertUser(user, pass);

        msgLabel.setStyle("-fx-text-fill: green;");
        msgLabel.setText("Đăng ký thành công! Đang chuyển về đăng nhập...");

        // Tự chuyển về Login sau 1.5 giây
        new Thread(() -> {
            try {
                Thread.sleep(1500);
                javafx.application.Platform.runLater(() -> {
                    try { goToLogin(); } catch (Exception e) { e.printStackTrace(); }
                });
            } catch (InterruptedException e) { e.printStackTrace(); }
        }).start();
    }

    @FXML
    private void togglePassword() {
        passShown = !passShown;
        if (passShown) {
            passwordVisible.setText(passwordField.getText());
            passwordVisible.setVisible(true);
            passwordVisible.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            toggleRegPass.setText("🙈");
        } else {
            passwordField.setText(passwordVisible.getText());
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordVisible.setVisible(false);
            passwordVisible.setManaged(false);
            toggleRegPass.setText("👁");
        }
    }

    @FXML
    private void goToLogin() throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/example/demo/login.fxml")      //load lại hàm login
        );
        Stage stage = (Stage) backBtn.getScene().getWindow();
        stage.setScene(new Scene(loader.load(), 400, 350));
    }
}