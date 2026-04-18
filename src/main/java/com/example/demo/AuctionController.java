package com.example.demo;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuctionController {

    @FXML private Label titleLabel, currentPriceLabel, timerLabel;
    @FXML private Label statusBadge, bidErrorLabel, leaderLabel;
    @FXML private TextField bidField;
    @FXML private Button backBtn, bidBtn;
    @FXML private VBox bidHistoryBox;

    private DashboardController.AuctionItem item;
    private User currentUser;
    private double currentPrice;
    private String leader = "Chưa có";

    public void setData(DashboardController.AuctionItem item, User user) {
        this.item = item;
        this.currentUser = user;
        this.currentPrice = item.currentPrice;

        titleLabel.setText(item.name);
        updatePrice(currentPrice);
        updateTimer();

        boolean isOpen = item.status.equals("OPEN");
        statusBadge.setText(isOpen ? "Đang mở" : "Đã kết thúc");
        bidBtn.setDisable(!isOpen);

        addBidHistory("Hệ thống", item.currentPrice, "Giá khởi điểm");
    }

    private void updatePrice(double price) {
        currentPriceLabel.setText(String.format("%,.0f đ", price));
        leaderLabel.setText(leader);
    }

    private void updateTimer() {
        if (item == null) return;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
        timerLabel.setText("Kết thúc lúc: " + item.endTime.format(fmt));
    }

    @FXML
    private void handleBid() {
        String input = bidField.getText().trim().replaceAll(",", "");

        if (input.isEmpty()) {
            bidErrorLabel.setText("Vui lòng nhập số tiền!");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(input);
        } catch (NumberFormatException e) {
            bidErrorLabel.setText("Số tiền không hợp lệ!");
            return;
        }

        if (amount <= currentPrice) {
            bidErrorLabel.setText("Giá phải cao hơn " + String.format("%,.0f đ", currentPrice));
            return;
        }

        // Đặt giá thành công
        bidErrorLabel.setText("");
        currentPrice = amount;
        leader = currentUser != null ? currentUser.getName() : "Bạn";
        updatePrice(currentPrice);
        addBidHistory(leader, amount, "");
        bidField.clear();
    }

    private void addBidHistory(String name, double amount, String note) {
        HBox row = new HBox(10);
        row.setStyle("-fx-background-color: #2d2d2d; -fx-padding: 10; -fx-background-radius: 8;");

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-text-fill: #afa9ec; -fx-font-size: 13px; -fx-font-weight: bold;");
        nameLabel.setPrefWidth(120);

        Label amountLabel = new Label(String.format("%,.0f đ", amount));
        amountLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String time = note.isEmpty()
                ? LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                : note;
        Label timeLabel = new Label(time);
        timeLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");

        row.getChildren().addAll(nameLabel, amountLabel, spacer, timeLabel);
        bidHistoryBox.getChildren().add(0, row); // thêm mới nhất lên đầu
    }

    @FXML
    private void handleBack() throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/example/demo/dashboard.fxml")
        );
        Stage stage = (Stage) backBtn.getScene().getWindow();
        stage.setScene(new Scene(loader.load(), 800, 600));

        DashboardController dc = loader.getController();
        dc.setUser(currentUser);
    }
}