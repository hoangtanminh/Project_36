package com.auction.model;

public class Auction {
    private Item item;
    private Bid highestBid;
    private boolean isOpen;

    public Auction(Item item) {
        this.item = item;
        this.isOpen = true;
    }

    public synchronized void placeBid(Bid bid) {

        if (!isOpen) {
            throw new RuntimeException("Auction is closed");
        }

        if (bid.getAmount() <= item.getCurrentPrice()) {
            throw new RuntimeException("Invalid bid: must be higher than current price");
        }

        item.setCurrentPrice(bid.getAmount());

        highestBid = bid;

        System.out.println("New highest bid: " + bid.getAmount()
                + " by " + bid.getBidder().getName());
    }
    public void closeAuction() {
        isOpen = false;
    }

    public Bid getHighestBid() {
        return highestBid;
    }

    public Item getItem() {
        return item;
    }

    public boolean isOpen() {
        return isOpen;
    }
}