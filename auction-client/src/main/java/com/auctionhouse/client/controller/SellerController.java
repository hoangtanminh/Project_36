package com.auctionhouse.client.controller;

import com.auctionhouse.client.service.AuctionClientService;
import com.auctionhouse.client.view.AppCoordinator;
import com.auctionhouse.shared.enums.AuctionStatus;
import com.auctionhouse.shared.enums.UserRole;
import com.auctionhouse.shared.model.Auction;
import com.auctionhouse.shared.model.CollectibleProduct;
import com.auctionhouse.shared.model.ElectronicsProduct;
import com.auctionhouse.shared.model.User;
import com.auctionhouse.shared.protocol.AuctionActionRequest;
import com.auctionhouse.shared.protocol.AuctionStatusChangeRequest;
import com.auctionhouse.shared.protocol.CreateAuctionRequest;
import com.auctionhouse.shared.protocol.UpdateAuctionRequest;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class SellerController {
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(Locale.US);

    @FXML private Label pageTitle;
    @FXML private Label actionStatusLabel;
    @FXML private ListView<Auction> myAuctionListView;
    @FXML private ChoiceBox<String> productTypeChoice;
    @FXML private TextField createTitleField;
    @FXML private TextArea createDescriptionArea;
    @FXML private TextField createCategoryField;
    @FXML private TextField createImageHintField;
    @FXML private TextField createOpeningPriceField;
    @FXML private TextField createReservePriceField;
    @FXML private TextField createIncrementField;
    @FXML private Spinner<Integer> durationSpinner;
    @FXML private TextField createSecondaryField;
    @FXML private TextField createTertiaryField;
    @FXML private VBox adminPanel;

    private AppCoordinator coordinator;
    private AuctionClientService clientService;
    private User currentUser;
    private Auction selectedAuction;

    public void init(AppCoordinator coordinator, AuctionClientService clientService, User user) {
        this.coordinator = coordinator;
        this.clientService = clientService;
        this.currentUser = user;
        configureView();
        loadMyAuctions();
    }

    @FXML
    private void goBack() {
        try { coordinator.showDashboard(currentUser); }
        catch (Exception e) { actionStatusLabel.setText(e.getMessage()); }
    }

    @FXML
    private void createAuction() {
        try {
            CreateAuctionRequest req = new CreateAuctionRequest(
                    currentUser.getUsername(),
                    productTypeChoice.getValue(),
                    createTitleField.getText().trim(),
                    createDescriptionArea.getText().trim(),
                    createCategoryField.getText().trim(),
                    createImageHintField.getText().trim(),
                    new BigDecimal(createOpeningPriceField.getText().trim()),
                    new BigDecimal(createReservePriceField.getText().trim()),
                    new BigDecimal(createIncrementField.getText().trim()),
                    durationSpinner.getValue(),
                    createSecondaryField.getText().trim(),
                    createTertiaryField.getText().trim());

            actionStatusLabel.setText("Publishing auction...");
            CompletableFuture.supplyAsync(() -> clientService.createAuction(req))
                    .whenComplete((auction, err) -> Platform.runLater(() -> {
                        if (err != null) { actionStatusLabel.setText(extractMessage(err)); return; }
                        actionStatusLabel.setText("Auction created!");
                        clearForm();
                        loadMyAuctions();
                    }));
        } catch (Exception e) {
            actionStatusLabel.setText("Fill all fields with valid values.");
        }
    }

    @FXML
    private void updateAuction() {
        if (selectedAuction == null) {
            actionStatusLabel.setText("Select an auction first.");
            return;
        }
        try {
            UpdateAuctionRequest req = new UpdateAuctionRequest(
                    selectedAuction.getId(),
                    currentUser.getUsername(),
                    productTypeChoice.getValue(),
                    createTitleField.getText().trim(),
                    createDescriptionArea.getText().trim(),
                    createCategoryField.getText().trim(),
                    createImageHintField.getText().trim(),
                    new BigDecimal(createOpeningPriceField.getText().trim()),
                    new BigDecimal(createReservePriceField.getText().trim()),
                    new BigDecimal(createIncrementField.getText().trim()),
                    durationSpinner.getValue(),
                    createSecondaryField.getText().trim(),
                    createTertiaryField.getText().trim());

            actionStatusLabel.setText("Updating...");
            CompletableFuture.supplyAsync(() -> clientService.updateAuction(req))
                    .whenComplete((auction, err) -> Platform.runLater(() -> {
                        if (err != null) { actionStatusLabel.setText(extractMessage(err)); return; }
                        actionStatusLabel.setText("Updated!");
                        loadMyAuctions();
                    }));
        } catch (Exception e) {
            actionStatusLabel.setText("Fill all fields with valid values.");
        }
    }

    @FXML
    private void deleteAuction() {
        if (selectedAuction == null) { actionStatusLabel.setText("Select an auction first."); return; }
        actionStatusLabel.setText("Deleting...");
        CompletableFuture.runAsync(
                        () -> clientService.deleteAuction(
                                new AuctionActionRequest(selectedAuction.getId(), currentUser.getUsername())))
                .whenComplete((v, err) -> Platform.runLater(() -> {
                    if (err != null) { actionStatusLabel.setText(extractMessage(err)); return; }
                    selectedAuction = null;
                    clearForm();
                    actionStatusLabel.setText("Deleted!");
                    loadMyAuctions();
                }));
    }

    @FXML
    private void startAuction() { changeStatus(AuctionStatus.RUNNING, "Starting..."); }
    @FXML
    private void finishAuction() { changeStatus(AuctionStatus.FINISHED, "Finishing..."); }
    @FXML
    private void markAuctionPaid() { changeStatus(AuctionStatus.PAID, "Marking paid..."); }
    @FXML
    private void cancelAuction() { changeStatus(AuctionStatus.CANCELED, "Canceling..."); }

    private void changeStatus(AuctionStatus status, String msg) {
        if (selectedAuction == null) { actionStatusLabel.setText("Select an auction first."); return; }
        actionStatusLabel.setText(msg);
        CompletableFuture.supplyAsync(
                        () -> clientService.changeAuctionStatus(
                                new AuctionStatusChangeRequest(selectedAuction.getId(), currentUser.getUsername(), status)))
                .whenComplete((auction, err) -> Platform.runLater(() -> {
                    if (err != null) { actionStatusLabel.setText(extractMessage(err)); return; }
                    actionStatusLabel.setText("Status → " + status);
                    loadMyAuctions();
                }));
    }

    private void configureView() {
        pageTitle.setText(currentUser.getRole() == UserRole.ADMIN ? "Admin Panel" : "Seller Panel");
        productTypeChoice.getItems().setAll("electronics", "collectible");
        productTypeChoice.setValue("electronics");
        durationSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 720, 60, 5));

        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        adminPanel.setVisible(isAdmin);
        adminPanel.setManaged(isAdmin);

        myAuctionListView.setCellFactory(lv -> new ListCell<>() {
            private final Label titleLabel = new Label();
            private final Label metaLabel = new Label();
            private final VBox content = new VBox(titleLabel, metaLabel);

            {
                titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: white;");
                metaLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #b7bfd8;");
                content.setSpacing(4);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }

            @Override
            protected void updateItem(Auction item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }
                titleLabel.setText(item.getProduct().getTitle());
                metaLabel.setText(item.getStatus() + " | " + CURRENCY.format(item.getCurrentPrice()));
                setGraphic(content);
                setStyle("-fx-background-color: #111522; -fx-background-radius: 10; "
                        + "-fx-border-color: #2b3047; -fx-border-radius: 10; -fx-padding: 10 12;");
            }
        });

        myAuctionListView.getSelectionModel().selectedItemProperty()
                .addListener((obs, o, n) -> { if (n != null) { selectedAuction = n; populateForm(n); } });
    }

    private void loadMyAuctions() {
        CompletableFuture.supplyAsync(
                        () -> clientService.loadDashboard(currentUser.getUsername()))
                .whenComplete((data, err) -> Platform.runLater(() -> {
                    if (err != null) { actionStatusLabel.setText(extractMessage(err)); return; }
                    myAuctionListView.getItems().setAll(data.sellerAuctions());
                }));
    }

    private void populateForm(Auction auction) {
        createTitleField.setText(auction.getProduct().getTitle());
        createDescriptionArea.setText(auction.getProduct().getDescription());
        createCategoryField.setText(auction.getProduct().getCategory());
        createImageHintField.setText(auction.getProduct().getImageHint());
        createOpeningPriceField.setText(auction.getProduct().getOpeningPrice().toPlainString());
        createReservePriceField.setText(auction.getReservePrice().toPlainString());
        createIncrementField.setText(
                auction.getMinimumNextBid().subtract(auction.getCurrentPrice()).toPlainString());
        durationSpinner.getValueFactory().setValue(
                (int) Math.max(5, Duration.between(auction.getStartTime(), auction.getEndTime()).toMinutes()));

        if (auction.getProduct() instanceof CollectibleProduct c) {
            productTypeChoice.setValue("collectible");
            createSecondaryField.setText(c.getEra());
            createTertiaryField.setText(c.getAuthenticityGrade());
        } else if (auction.getProduct() instanceof ElectronicsProduct e) {
            productTypeChoice.setValue("electronics");
            createSecondaryField.setText(e.getBrand());
            createTertiaryField.setText(e.getCondition());
        }
    }

    private void clearForm() {
        createTitleField.clear(); createDescriptionArea.clear();
        createCategoryField.clear(); createImageHintField.clear();
        createOpeningPriceField.clear(); createReservePriceField.clear();
        createIncrementField.clear(); createSecondaryField.clear();
        createTertiaryField.clear();
        durationSpinner.getValueFactory().setValue(60);
        productTypeChoice.setValue("electronics");
    }

    private String extractMessage(Throwable t) {
        Throwable c = t.getCause() == null ? t : t.getCause();
        return c.getMessage() == null ? t.getMessage() : c.getMessage();
    }
}
