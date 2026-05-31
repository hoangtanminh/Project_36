package com.auctionhouse.client.model;

import com.auction.model.Auction;
import com.auction.shared.dto.AuctionMetrics;
import com.auction.model.Bid;
import com.auction.shared.dto.DashboardView;
import com.auction.model.User;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public final class SessionModel {
    private final ObjectProperty<User> currentUser = new SimpleObjectProperty<>();
    private final ObjectProperty<Auction> selectedAuction = new SimpleObjectProperty<>();
    private final ObjectProperty<AuctionMetrics> metrics = new SimpleObjectProperty<>();
    private final ObservableList<Auction> liveAuctions = FXCollections.observableArrayList();
    private final ObservableList<Auction> sellerAuctions = FXCollections.observableArrayList();
    private final ObservableList<Bid> bidHistory = FXCollections.observableArrayList();
    private final StringProperty toastMessage = new SimpleStringProperty("");

    public void applyDashboard(DashboardView data) {
        currentUser.set(data.currentUser());
        liveAuctions.setAll(data.liveAuctions());
        sellerAuctions.setAll(data.sellerAuctions());
        metrics.set(data.metrics());
    }

    public void selectAuction(Auction auction) {
        selectedAuction.set(auction);
        if (auction == null) {
            bidHistory.clear();
            return;
        }
        bidHistory.setAll(auction.getBidHistory());
    }

    public ObjectProperty<User> currentUserProperty() {
        return currentUser;
    }

    public ObjectProperty<Auction> selectedAuctionProperty() {
        return selectedAuction;
    }

    public ObjectProperty<AuctionMetrics> metricsProperty() {
        return metrics;
    }

    public ObservableList<Auction> getLiveAuctions() {
        return liveAuctions;
    }

    public ObservableList<Auction> getSellerAuctions() {
        return sellerAuctions;
    }

    public ObservableList<Bid> getBidHistory() {
        return bidHistory;
    }

    public StringProperty toastMessageProperty() {
        return toastMessage;
    }
}

