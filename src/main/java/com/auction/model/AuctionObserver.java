package com.auction.model;

public interface AuctionObserver {
    void onNewBid(Auction auction, Bid bid);
}
