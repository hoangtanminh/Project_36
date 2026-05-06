package com.auctionhouse.client.controller;

import com.auctionhouse.client.model.SessionModel;
import com.auctionhouse.client.service.AuctionClientService;
import com.auctionhouse.client.view.AppCoordinator;
import com.auctionhouse.shared.enums.AuctionEventType;
import com.auctionhouse.shared.enums.UserRole;
import com.auctionhouse.shared.model.Auction;
import com.auctionhouse.shared.model.AuctionMetrics;
import com.auctionhouse.shared.model.User;
import com.auctionhouse.shared.protocol.ServerResponse;
import javafx.application.Platform;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.text.NumberFormat;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class DashboardController {
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(Locale.US);

    @FXML private Label currentUserLabel;
    @FXML private Label roleBadgeLabel;
    @FXML private Label liveMetricLabel;
    @FXML private Label sellerMetricLabel;
    @FXML private Label bidMetricLabel;
    @FXML private TextField searchField;
    @FXML private ChoiceBox<String> categoryChoice;
    @FXML private ListView<Auction> auctionListView;
    @FXML private Label actionStatusLabel;
    @FXML private Button sellerBtn;

    private final SessionModel sessionModel = new SessionModel();
    private final FilteredList<Auction> filteredAuctions =
            new FilteredList<>(sessionModel.getLiveAuctions(), auction -> true);

    private AppCoordinator coordinator;
    private AuctionClientService clientService;
    private User currentUser;

    public void init(AppCoordinator coordinator, AuctionClientService clientService, User currentUser) {
        this.coordinator = coordinator;
        this.clientService = clientService;
        this.currentUser = currentUser;
        configureView();
        clientService.setEventListener(this::handleEventResponse);
        loadDashboard();
    }

    @FXML
    private void refreshDashboard() {
        actionStatusLabel.setText("Refreshing auctions...");
        loadDashboard();
    }

    @FXML
    private void goToSeller() {
        try {
            coordinator.showSeller(currentUser);
        } catch (Exception exception) {
            actionStatusLabel.setText(exception.getMessage());
        }
    }

    private void configureView() {
        currentUserLabel.setText(currentUser.getDisplayName());
        roleBadgeLabel.setText(currentUser.getRoleLabel());
        actionStatusLabel.setText("Select an auction to view details.");

        boolean isSeller = currentUser.getRole() == UserRole.SELLER;
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        sellerBtn.setVisible(isSeller || isAdmin);
        sellerBtn.setManaged(isSeller || isAdmin);

        categoryChoice.getItems().setAll("All categories");
        categoryChoice.setValue("All categories");

        Label placeholder = new Label("No auctions match the current filters.");
        placeholder.setStyle("-fx-text-fill: #6a7693; -fx-font-size: 12px;");
        auctionListView.setPlaceholder(placeholder);
        auctionListView.setItems(filteredAuctions);
        auctionListView.setCellFactory(listView -> new ListCell<>() {
            private final Label titleLabel = new Label();
            private final Label metaLabel = new Label();
            private final Label hintLabel = new Label("Open detail view");
            private final VBox content = new VBox(titleLabel, metaLabel, hintLabel);

            {
                titleLabel.setStyle("-fx-font-size: 19px; -fx-font-weight: bold; -fx-text-fill: white;");
                metaLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #b7bfd8;");
                hintLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #5dcaa5;");
                content.setSpacing(5);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setOnMouseClicked(event -> {
                    if (!isEmpty() && getItem() != null) {
                        openAuction(getItem());
                    }
                });
            }

            @Override
            protected void updateItem(Auction item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }

                titleLabel.setText(item.getProduct().getTitle());
                metaLabel.setText(item.getProduct().getCategory()
                        + " | " + item.getStatus()
                        + " | " + CURRENCY.format(item.getCurrentPrice()));
                hintLabel.setText(item.getStatus().name() + " auction");
                setGraphic(content);
                applyCellStyle(isSelected());
            }

            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                applyCellStyle(selected);
            }

            private void applyCellStyle(boolean selected) {
                if (selected) {
                    setStyle("-fx-background-color: #243a57; -fx-background-radius: 12; "
                            + "-fx-border-color: #35a9d4; -fx-border-radius: 12; "
                            + "-fx-padding: 12 14; -fx-cursor: hand;");
                } else {
                    setStyle("-fx-background-color: #111522; -fx-background-radius: 12; "
                            + "-fx-border-color: #2b3047; -fx-border-radius: 12; "
                            + "-fx-padding: 12 14; -fx-cursor: hand;");
                }
            }
        });

        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        categoryChoice.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> applyFilters());
    }

    private void openAuction(Auction auction) {
        try {
            actionStatusLabel.setText("Opening auction detail...");
            coordinator.showAuctionDetail(currentUser, auction);
        } catch (Exception exception) {
            actionStatusLabel.setText(exception.getMessage());
        }
    }

    private void loadDashboard() {
        CompletableFuture.supplyAsync(() -> clientService.loadDashboard(currentUser.getUsername()))
                .whenComplete((data, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        actionStatusLabel.setText(extractMessage(error));
                        return;
                    }
                    sessionModel.applyDashboard(data);
                    updateMetrics(sessionModel.metricsProperty().get());
                    populateCategories();
                    applyFilters();
                    actionStatusLabel.setText("Select an auction to view details.");
                }));
    }

    private void populateCategories() {
        Set<String> categories = new LinkedHashSet<>();
        categories.add("All categories");
        sessionModel.getLiveAuctions().forEach(auction -> categories.add(auction.getProduct().getCategory()));
        String currentCategory = categoryChoice.getValue();
        categoryChoice.getItems().setAll(categories);
        categoryChoice.setValue(categories.contains(currentCategory) ? currentCategory : "All categories");
    }

    private void applyFilters() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String category = categoryChoice.getValue();
        filteredAuctions.setPredicate(auction -> {
            boolean categoryMatches = category == null
                    || "All categories".equals(category)
                    || auction.getProduct().getCategory().equalsIgnoreCase(category);
            boolean keywordMatches = keyword.isBlank()
                    || auction.getProduct().getTitle().toLowerCase().contains(keyword)
                    || auction.getProduct().getDescription().toLowerCase().contains(keyword);
            return categoryMatches && keywordMatches;
        });
    }

    private void updateMetrics(AuctionMetrics metrics) {
        if (metrics == null) {
            return;
        }
        liveMetricLabel.setText(String.valueOf(metrics.liveAuctionCount()));
        sellerMetricLabel.setText(String.valueOf(metrics.sellerAuctionCount()));
        bidMetricLabel.setText(String.valueOf(metrics.totalBidCount()));
    }

    private void handleEventResponse(ServerResponse<?> response) {
        Platform.runLater(() -> {
            AuctionEventType eventType = response.getEventType();
            if (eventType == AuctionEventType.AUCTION_CREATED
                    || eventType == AuctionEventType.AUCTION_UPDATED
                    || eventType == AuctionEventType.AUCTION_DELETED
                    || eventType == AuctionEventType.AUCTION_STATUS_CHANGED
                    || eventType == AuctionEventType.AUCTION_CLOSED
                    || eventType == AuctionEventType.BID_PLACED) {
                loadDashboard();
            }
        });
    }

    private String extractMessage(Throwable throwable) {
        Throwable cause = throwable.getCause() == null ? throwable : throwable.getCause();
        return cause.getMessage() == null ? throwable.getMessage() : cause.getMessage();
    }
}
