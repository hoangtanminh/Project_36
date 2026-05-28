package com.auction.model.exception;

import com.auction.model.AuctionStatus;

public class AuctionClosedException extends IllegalStateException {
    private final String auctionId;
    private final AuctionStatus status;

    public AuctionClosedException(String auctionId, AuctionStatus status) {
        super(String.format("Auction '%s' is not open for bidding (status=%s).", auctionId, status));
        this.auctionId = auctionId;
        this.status = status;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public AuctionStatus getStatus() {
        return status;
    }
}