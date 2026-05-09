package com.auction.model;

import java.time.LocalDateTime;

public class Bid {

    private final Bidder bidder;
    private final double amount;
    private final LocalDateTime timestamp;

    public Bid(Bidder bidder, double amount) {
        this.bidder = bidder;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }

    public double getAmount() {
        return amount;
    }

    public Bidder getBidder() {
        return bidder;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}