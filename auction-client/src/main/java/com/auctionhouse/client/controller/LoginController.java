package com.auctionhouse.client.controller;

import com.auctionhouse.client.service.AuctionClientService;
import com.auctionhouse.client.view.AppCoordinator;
import com.auctionhouse.shared.model.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.concurrent.CompletableFuture;

public final class LoginController {
    @FXML                                           //sử dụng các component của fxml
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label statusLabel;
    @FXML
    private Button loginButton;

    private AppCoordinator coordinator;
    private AuctionClientService clientService;
        // init() được Appcoordinator gọi sau  khi load xong fxml
    public void init(AppCoordinator coordinator, AuctionClientService clientService) {
        this.coordinator = coordinator;
        this.clientService = clientService;
        statusLabel.setText("Sample login: bidder01/bidder01, bidder02/bidder02, seller01/seller01, admin01/admin01");
    }

    @FXML
    private void handleLogin() {
        // lấy text
        String username = readText(usernameField);
        String password = readText(passwordField);
        if (username.isBlank() || password.isBlank()) {     //isBlank để kiểm tra chuỗi rỗng
            statusLabel.setText("Enter both username and password.");
            return;     //  nếu rỗng thì dừng lại không chạy tiếp
        }

        loginButton.setDisable(true);       // vô hiệu nút login tránh việc spam
        statusLabel.setText("Connecting to auction server...");     //hiển thị đang kết nối sever
        //  CompletableFuture.supplyAsync() chạy code ở luồng phụ, xong rồi Platform.runLater() đưa kết quả về luồng UI để cập nhật giao diện.
        CompletableFuture.supplyAsync(() -> clientService.login(username, password))
                //khi chạy xong chạy tiếp
                .whenComplete((user, throwable) -> Platform.runLater(() -> finishLogin(user, throwable)));
    }

    @FXML       //bấm nút trong UI sẽ gọi đếnn hàm này
    private void goToRegister() {
        try {
            coordinator.showRegister();     // chuyển scene sang register
        } catch (Exception exception) {
            statusLabel.setText(exception.getMessage());        // lỗi thì báo ra UI
        }
    }

    @FXML
    private void loginAsBidder() {
        usernameField.setText("bidder01");
        passwordField.setText("bidder01");
        handleLogin();
    }

    @FXML
    private void loginAsSeller() {
        usernameField.setText("seller01");
        passwordField.setText("seller01");
        handleLogin();
    }

    @FXML
    private void loginAsAdmin() {
        usernameField.setText("admin01");
        passwordField.setText("admin01");
        handleLogin();
    }

    private void finishLogin(User user, Throwable throwable) {
        loginButton.setDisable(false);      // cho phép user bấm lại
        if (throwable != null) {
            statusLabel.setText(extractMessage(throwable));     // nếu có lỗi hiện thông báo ra UI
            return;
        }

        try {
            coordinator.showDashboard(user);    //chuyển sang màn hình chính
        } catch (Exception exception) {
            statusLabel.setText(exception.getMessage());    // lỗi khi load UI thì hiện massage
        }
    }

    private String readText(TextField textField) {
        return textField.getText() == null ? "" : textField.getText().trim();
    }

    private String extractMessage(Throwable throwable) {
        Throwable cause = throwable.getCause() == null ? throwable : throwable.getCause();
        return cause.getMessage() == null ? throwable.getMessage() : cause.getMessage();
    }
}
