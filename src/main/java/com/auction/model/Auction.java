
package com.auction.model;

public class Auction {
    private Item item;
    private Bid highestBid;

    public Auction(Item item) {
        this.item = item;
    }

    public void placeBid(Bid bid) {
        if (bid.getAmount() <= item.getCurrentPrice()) {
            throw new RuntimeException("Invalid bid");
        }
        item.setCurrentPrice(bid.getAmount());
        highestBid = bid;
    }

    public Bid getHighestBid() {
        return highestBid;
    }
}