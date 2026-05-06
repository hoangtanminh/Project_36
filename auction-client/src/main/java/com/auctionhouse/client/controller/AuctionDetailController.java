package com.auctionhouse.client.controller;

import com.auctionhouse.client.model.SessionModel;
import com.auctionhouse.client.service.AuctionClientService;
import com.auctionhouse.client.view.AppCoordinator;
import com.auctionhouse.shared.enums.AuctionStatus;
import com.auctionhouse.shared.enums.UserRole;
import com.auctionhouse.shared.model.Auction;
import com.auctionhouse.shared.model.Bid;
import com.auctionhouse.shared.model.BuyNowAuction;
import com.auctionhouse.shared.model.User;
import com.auctionhouse.shared.protocol.AuctionSubscriptionRequest;
import com.auctionhouse.shared.protocol.BidRequest;
import com.auctionhouse.shared.protocol.ServerResponse;
import com.auctionhouse.shared.enums.AuctionEventType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class AuctionDetailController {
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(Locale.US);

    @FXML private Label titleLabel;
    @FXML private Label sellerLabel;
    @FXML private Label categoryLabel;
    @FXML private Label typeLabel;
    @FXML private Label statusValueLabel;
    @FXML private Label countdownLabel;
    @FXML private Label priceLabel;
    @FXML private Label nextBidLabel;
    @FXML private Label reserveLabel;
    @FXML private Label winnerValueLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label productHighlightLabel;
    @FXML private Label imageHintLabel;
    @FXML private TextField bidAmountField;
    @FXML private Button placeBidButton;
    @FXML private Label actionStatusLabel;
    @FXML private ListView<Bid> bidHistoryListView;

    private AppCoordinator coordinator;
    private AuctionClientService clientService;
    private User currentUser;
    private final SessionModel sessionModel = new SessionModel();

    public void init(AppCoordinator coordinator, AuctionClientService clientService,
                     User currentUser, Auction auction) {
        this.coordinator = coordinator;
        this.clientService = clientService;
        this.currentUser = currentUser;

        configureView();
        clientService.setEventListener(this::handleEventResponse);
        subscribeAndRender(auction);
    }

    @FXML
    private void goBack() {
        try {
            coordinator.showDashboard(currentUser);
        } catch (Exception e) {
            actionStatusLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void placeBid() {
        Auction selected = sessionModel.selectedAuctionProperty().get();
        if (selected == null) {
            actionStatusLabel.setText("Auction details are still loading.");
            return;
        }

        try {
            if (currentUser.getRole() != UserRole.BIDDER) {
                actionStatusLabel.setText("Only bidder accounts can place bids.");
                return;
            }
            if (!selected.canAcceptBids()) {
                actionStatusLabel.setText("Auction is not accepting bids right now.");
                return;
            }

            BigDecimal amount = parseBidAmount(bidAmountField.getText());
            if (amount.compareTo(selected.getMinimumNextBid()) < 0) {
                actionStatusLabel.setText("Bid must be at least "
                        + CURRENCY.format(selected.getMinimumNextBid()));
                return;
            }

            actionStatusLabel.setText("Submitting bid...");
            CompletableFuture.supplyAsync(
                            () -> clientService.placeBid(
                                    new BidRequest(selected.getId(), currentUser.getUsername(), amount)))
                    .whenComplete((auction, err) -> Platform.runLater(() -> {
                        if (err != null) {
                            actionStatusLabel.setText(extractMessage(err));
                            return;
                        }
                        actionStatusLabel.setText("Bid accepted!");
                        sessionModel.selectAuction(auction);
                        renderAuction(auction);
                        bidAmountField.clear();
                    }));
        } catch (IllegalArgumentException e) {
            actionStatusLabel.setText(e.getMessage());
        }
    }

    private void configureView() {
        bidAmountField.setDisable(currentUser.getRole() != UserRole.BIDDER);
        placeBidButton.setDisable(currentUser.getRole() != UserRole.BIDDER);

        bidHistoryListView.setCellFactory(lv -> new ListCell<>() {
            private final Label bidderLabel = new Label();
            private final Label metaLabel = new Label();
            private final VBox content = new VBox(bidderLabel, metaLabel);

            {
                bidderLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
                metaLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #b7bfd8;");
                content.setSpacing(4);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }

            @Override
            protected void updateItem(Bid item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }
                bidderLabel.setText(item.getBidderDisplayName() + " placed " + CURRENCY.format(item.getAmount()));
                metaLabel.setText(item.getCreatedAt().toString());
                setGraphic(content);
                setStyle("-fx-background-color: #111522; -fx-background-radius: 10; "
                        + "-fx-border-color: #2b3047; -fx-border-radius: 10; -fx-padding: 10 12;");
            }
        });
        bidHistoryListView.setItems(sessionModel.getBidHistory());
    }

    private void subscribeAndRender(Auction auction) {
        CompletableFuture.supplyAsync(
                        () -> clientService.subscribe(auction.getId(), currentUser.getUsername()))
                .whenComplete((loaded, err) -> Platform.runLater(() -> {
                    if (err != null) {
                        actionStatusLabel.setText(extractMessage(err));
                        return;
                    }
                    sessionModel.selectAuction(loaded);
                    renderAuction(loaded);
                }));
    }

    private void renderAuction(Auction auction) {
        if (auction == null) return;
        titleLabel.setText(auction.getProduct().getTitle());
        sellerLabel.setText(auction.getSeller().getDisplayName());
        categoryLabel.setText(auction.getProduct().getCategory());
        typeLabel.setText(auction.getAuctionTypeLabel());
        statusValueLabel.setText(auction.getStatus().name());
        countdownLabel.setText(switch (auction.getStatus()) {
            case RUNNING -> auction.getSecondsRemaining() + " seconds remaining";
            case OPEN -> "Waiting to start";
            case FINISHED -> "Auction finished";
            case PAID -> "Payment completed";
            case CANCELED -> "Auction canceled";
        });
        priceLabel.setText(CURRENCY.format(auction.getCurrentPrice()));
        nextBidLabel.setText(CURRENCY.format(auction.getMinimumNextBid()));
        reserveLabel.setText(CURRENCY.format(auction.getReservePrice()));
        winnerValueLabel.setText(auction.getWinnerDisplayName());
        descriptionLabel.setText(auction.getProduct().getDescription());
        productHighlightLabel.setText(auction.getProduct().getHighlightLine());
        imageHintLabel.setText("Visual hint: " + auction.getProduct().getImageHint());
        updateBidInteractionState(auction);

        if (auction instanceof BuyNowAuction b) {
            typeLabel.setText(auction.getAuctionTypeLabel()
                    + " | Buy now " + CURRENCY.format(b.getBuyNowPrice()));
        }
    }

    private void updateBidInteractionState(Auction auction) {
        boolean canBid = currentUser.getRole() == UserRole.BIDDER && auction.canAcceptBids();
        bidAmountField.setDisable(!canBid);
        placeBidButton.setDisable(!canBid);
        bidAmountField.setPromptText("Min " + CURRENCY.format(auction.getMinimumNextBid()));

        if (currentUser.getRole() != UserRole.BIDDER) {
            actionStatusLabel.setText("Only bidder accounts can place bids.");
            return;
        }
        if (auction.getStatus() != AuctionStatus.RUNNING) {
            actionStatusLabel.setText("Auction must be RUNNING before you can bid.");
            return;
        }
        if (!auction.canAcceptBids()) {
            actionStatusLabel.setText("Auction is no longer accepting bids.");
        }
    }

    private void handleEventResponse(ServerResponse<?> response) {
        Platform.runLater(() -> {
            actionStatusLabel.setText(response.getMessage());
            if (response.getPayload() instanceof Auction auction) {
                Auction cur = sessionModel.selectedAuctionProperty().get();
                if (cur != null && cur.getId() == auction.getId()) {
                    sessionModel.selectAuction(auction);
                    renderAuction(auction);
                }
            }
        });
    }

    private String extractMessage(Throwable t) {
        Throwable c = t.getCause() == null ? t : t.getCause();
        return c.getMessage() == null ? t.getMessage() : c.getMessage();
    }

    private BigDecimal parseBidAmount(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("Enter a bid amount.");
        }

        String sanitized = rawValue.trim().replace(" ", "").replace("$", "");
        int lastComma = sanitized.lastIndexOf(',');
        int lastDot = sanitized.lastIndexOf('.');
        if (lastComma >= 0 && lastDot >= 0) {
            if (lastComma > lastDot) {
                sanitized = sanitized.replace(".", "").replace(',', '.');
            } else {
                sanitized = sanitized.replace(",", "");
            }
        } else if (lastComma >= 0) {
            int decimalDigits = sanitized.length() - lastComma - 1;
            sanitized = decimalDigits <= 2
                    ? sanitized.replace(',', '.')
                    : sanitized.replace(",", "");
        }

        try {
            return new BigDecimal(sanitized);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Enter a valid bid amount.");
        }
    }
}
