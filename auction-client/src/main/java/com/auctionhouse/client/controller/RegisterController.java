//register
package com.auctionhouse.client.controller;

import com.auctionhouse.client.service.AuctionClientService;
import com.auctionhouse.client.view.AppCoordinator;
import com.auctionhouse.shared.enums.UserRole;
import com.auctionhouse.shared.model.User;
import com.auctionhouse.shared.protocol.RegisterRequest;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.concurrent.CompletableFuture;

public final class RegisterController {
    @FXML
    private TextField displayNameField;                 //JavaFx gán các biến từ fxml vào controller
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private ChoiceBox<UserRole> roleChoice;
    @FXML
    private Label roleDescriptionLabel;
    @FXML
    private VBox storefrontBox;
    @FXML
    private TextField storefrontField;
    @FXML
    private Label statusLabel;
    @FXML
    private Button registerButton;

    private AppCoordinator coordinator;
    private AuctionClientService clientService;

    public void init(AppCoordinator coordinator, AuctionClientService clientService) {
        this.coordinator = coordinator;
        this.clientService = clientService;
        roleChoice.setConverter(new StringConverter<>() {   // converter giúp hiển thị UI của boxchoice đẹp hơn
            @Override
            public String toString(UserRole role) {
                return formatRoleLabel(role);
            }

            @Override
            public UserRole fromString(String value) {
                return null;
            }
        });
        roleChoice.getItems().setAll(UserRole.BIDDER, UserRole.SELLER);//set roleChoice gồm bidder và seller
        roleChoice.setValue(UserRole.BIDDER);   //mặc định set là bidder
        
        // khi user đổi role thì gọi hàm updateRoleSelection
        roleChoice.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> updateRoleSelection());
        updateRoleSelection();
        statusLabel.setText("Create a new account.");
    }

    @FXML
    private void handleRegister() {
        String displayName = readText(displayNameField);
        String username = readText(usernameField);
        String password = readText(passwordField);
        String confirmPassword = readText(confirmPasswordField);
        //nếu user chưa chọn thì để mặc định là Bidder
        UserRole role = roleChoice.getValue() == null ? UserRole.BIDDER : roleChoice.getValue();
        String storefrontName = readText(storefrontField);

        if (displayName.isBlank() || username.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            statusLabel.setText("Fill all required register fields.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            statusLabel.setText("Password confirmation does not match.");
            return;
        }
        if (role == UserRole.SELLER && storefrontName.isBlank()) {
            statusLabel.setText("Seller accounts need a storefront name.");
            return;
        }

        registerButton.setDisable(true);
        statusLabel.setText("Creating account...");
        RegisterRequest request = new RegisterRequest(username, password, displayName, role, storefrontName);
        CompletableFuture.supplyAsync(() -> clientService.register(request))
                .whenComplete((user, throwable) -> Platform.runLater(() -> finishRegister(user, throwable)));
    }
    //back lại login
    @FXML
    private void goToLogin() {
        try {
            coordinator.showLogin();
        } catch (Exception exception) {
            statusLabel.setText(exception.getMessage());
        }
    }
    // kết qả của đăng kí
    private void finishRegister(User user, Throwable throwable) {
        registerButton.setDisable(false);   // mở lại nút bấm
        if (throwable != null) {
            statusLabel.setText(extractMessage(throwable)); //có lỗi thì dừng và hiện massage
            return;
        }

        try {
            coordinator.showDashboard(user);
        } catch (Exception exception) {
            statusLabel.setText(exception.getMessage());
        }
    }
    // chọn chức năng cho account
    private void updateRoleSelection() {
        UserRole selectedRole = roleChoice.getValue();
        boolean sellerSelected = selectedRole == UserRole.SELLER;
        storefrontBox.setVisible(sellerSelected);
        storefrontBox.setManaged(sellerSelected);
        roleDescriptionLabel.setText(buildRoleDescription(selectedRole));
        if (!sellerSelected) {
            storefrontField.clear();        // xóa shop name tránh trường hợp khi đổi lại sang bidder dữ liệu còn
        }
    }

    private String formatRoleLabel(UserRole role) {
        if (role == null) {
            return "";          
        }
        return switch (role) {
            case BIDDER -> "Bidder Account";        // đổi hiện thị tương ứng với các chức năng
            case SELLER -> "Seller Account";
            case ADMIN -> "Admin Account";
        };
    }

    // trả về mô tả chi tiết của chức năng
    private String buildRoleDescription(UserRole role) {
        if (role == null) {
            return "";
        }
        return switch (role) {
            case BIDDER -> "Place bids, follow live auctions, and compete for items.";
            case SELLER -> "Create auctions, manage listings, and run your storefront.";
            case ADMIN -> "Administrative tools are not available for self-registration.";
        };
    }

    private String readText(TextField textField) {
        return textField.getText() == null ? "" : textField.getText().trim();   //lấy text đẹp
    }

    //lấy massage lỗi
    private String extractMessage(Throwable throwable) {
        Throwable cause = throwable.getCause() == null ? throwable : throwable.getCause();
        return cause.getMessage() == null ? throwable.getMessage() : cause.getMessage();
    }
}
