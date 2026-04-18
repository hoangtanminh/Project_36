package com.example.demo;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DashboardController {

    @FXML private Label userLabel;
    @FXML private GridPane gridPane;
    @FXML private Button btnAll, btnOpen, btnClosed, logoutBtn;

    private User currentUser;
    private List<AuctionItem> allAuctions = new ArrayList<>();

    public static class AuctionItem {
        public String id, name, category, status;
        public double currentPrice;
        public LocalDateTime endTime;

        public AuctionItem(String id, String name, String category,
                           double currentPrice, String status, LocalDateTime endTime) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.currentPrice = currentPrice;
            this.status = status;
            this.endTime = endTime;
        }
    }

    @FXML
    public void initialize() {
        loadSampleData();
        showAuctions(allAuctions);
    }

    public void setUser(User user) {
        this.currentUser = user;
        if (user != null) {
            userLabel.setText("Xin chào, " + user.getName());
        }
    }

    private void loadSampleData() {
        allAuctions.add(new AuctionItem("A01", "Laptop Gaming MSI", "Electronics",
                15500000, "OPEN", LocalDateTime.now().plusMinutes(12)));
        allAuctions.add(new AuctionItem("A02", "Tranh sơn dầu cổ điển", "Art",
                8200000, "OPEN", LocalDateTime.now().plusHours(2)));
        allAuctions.add(new AuctionItem("A03", "iPhone 15 Pro Max", "Electronics",
                22000000, "OPEN", LocalDateTime.now().plusMinutes(3)));
        allAuctions.add(new AuctionItem("A04", "Đồng hồ Rolex cổ", "Art",
                45000000, "CLOSED", LocalDateTime.now().minusHours(1)));
    }

    private void showAuctions(List<AuctionItem> list) {
        gridPane.getChildren().clear();
        gridPane.getColumnConstraints().clear();

        ColumnConstraints col = new ColumnConstraints();
        col.setPercentWidth(50);
        gridPane.getColumnConstraints().addAll(col, col);

        int c = 0, r = 0;
        for (AuctionItem item : list) {
            gridPane.add(createCard(item), c, r);
            c++;
            if (c == 2) { c = 0; r++; }
        }
    }

    private VBox createCard(AuctionItem item) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: #2d2d2d; -fx-padding: 14; -fx-background-radius: 10;");

        Label name = new Label(item.name);
        name.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        name.setWrapText(true);

        Label cat = new Label(item.category);
        cat.setStyle("-fx-background-color: #3d3d5c; -fx-text-fill: #afa9ec;" +
                "-fx-padding: 2 8 2 8; -fx-background-radius: 99; -fx-font-size: 11px;");

        Label priceLabel = new Label("Giá hiện tại");
        priceLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 11px;");

        Label price = new Label(String.format("%,.0f đ", item.currentPrice));
        price.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        boolean isOpen = item.status.equals("OPEN");

        Label badge = new Label(isOpen ? "Đang mở" : "Đã kết thúc");
        badge.setStyle("-fx-background-color: " + (isOpen ? "#0f6e56" : "#7d3030") +
                "; -fx-text-fill: " + (isOpen ? "#9fe1cb" : "#f09595") +
                "; -fx-padding: 2 8 2 8; -fx-background-radius: 99; -fx-font-size: 11px;");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm dd/MM");
        Label timer = new Label(isOpen
                ? "Kết thúc: " + item.endTime.format(fmt)
                : "Đã kết thúc");
        timer.setStyle("-fx-text-fill: " + (isOpen ? "#f39c12" : "#888888") + "; -fx-font-size: 11px;");

        Button btn = new Button(isOpen ? "Tham gia đấu giá" : "Xem kết quả");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color: " + (isOpen ? "#534ab7" : "#3d3d3d") +
                "; -fx-text-fill: white; -fx-cursor: hand;" +
                "-fx-font-size: 12px; -fx-background-radius: 6;");
        btn.setOnAction(e -> goToAuction(item));

        card.getChildren().addAll(name, cat, priceLabel, price, badge, timer, btn);
        return card;
    }

    private void goToAuction(AuctionItem item) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/demo/auction.fxml")
            );
            Stage stage = (Stage) logoutBtn.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 800, 600));

            AuctionController ac = loader.getController();
            ac.setData(item, currentUser);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML private void filterAll() {
        setActive(btnAll);
        showAuctions(allAuctions);
    }

    @FXML private void filterOpen() {
        setActive(btnOpen);
        showAuctions(allAuctions.stream()
                .filter(a -> a.status.equals("OPEN"))
                .collect(Collectors.toList()));
    }

    @FXML private void filterClosed() {
        setActive(btnClosed);
        showAuctions(allAuctions.stream()
                .filter(a -> a.status.equals("CLOSED"))
                .collect(Collectors.toList()));
    }

    private void setActive(Button active) {
        String normal = "-fx-background-color: #3d3d3d; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 20;";
        String activeStyle = "-fx-background-color: #7f77dd; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 20;";
        btnAll.setStyle(normal); btnOpen.setStyle(normal); btnClosed.setStyle(normal);
        active.setStyle(activeStyle);
    }

    @FXML
    private void handleLogout() throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/example/demo/login.fxml")
        );
        Stage stage = (Stage) logoutBtn.getScene().getWindow();
        stage.setScene(new Scene(loader.load(), 400, 350));
    }
}