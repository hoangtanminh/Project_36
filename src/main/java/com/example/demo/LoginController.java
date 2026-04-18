package com.example.demo;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField passwordVisible;
    @FXML
    private Label errorLabel;
    @FXML
    private Button loginBtn;
    @FXML
    private Button toggleLoginPass;

    private boolean passShown = false;

    @FXML
    private void handleLogin() throws Exception {

        String username = usernameField.getText();
        String password = passShown ? passwordVisible.getText() : passwordField.getText();

        if (UserService.login(username, password)) {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/demo/dashboard.fxml")
            );

            Parent root = loader.load();

            DashboardController controller = loader.getController();

            User user = new User("U01", username);
            controller.setUser(user);

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root));

        } else {
            errorLabel.setStyle("-fx-text-fill: red;");
            errorLabel.setText("Sai tài khoản hoặc mật khẩu!");
        }
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
            toggleLoginPass.setText("🙈");
        } else {
            passwordField.setText(passwordVisible.getText());
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordVisible.setVisible(false);
            passwordVisible.setManaged(false);
            toggleLoginPass.setText("👁");
        }
    }

    @FXML
    private void goToRegister() throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/example/demo/register.fxml")
        );
        Stage stage = (Stage) loginBtn.getScene().getWindow();
        stage.setScene(new Scene(loader.load(), 400, 350));
    }
}