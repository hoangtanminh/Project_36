package com.auction.model;

import com.auction.model.User.Bidder;

public class Bid {
    private Bidder bidder;
    private double amount;

    public Bid(Bidder bidder, double amount) {
        this.bidder = bidder;
        this.amount = amount;
    }

    public double getAmount() {
        return amount;
    }

    public Bidder getBidder() {
        return bidder;
    }
}