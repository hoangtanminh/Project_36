package com.auction.model;
import com.auction.model.Auction;

public interface Observer {
    void update(Auction auction);
}
