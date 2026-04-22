package com.auction.model.validation;

import java.time.LocalDateTime;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
import com.auction.model.AuctionState;
import com.auction.model.Bid;

public class DefaultBidValidationPolicy implements BidValidationPolicy {
    @Override
    public void validate(
            AuctionState state,
            LocalDateTime now,
            LocalDateTime startTime,
            LocalDateTime endTime,
            double currentPrice,
            Bid bid
    ) {
        if (bid == null || bid.getBidder() == null) {
            throw new IllegalArgumentException("Bid and bidder are required");
        }
        if (Double.isNaN(bid.getAmount()) || Double.isInfinite(bid.getAmount()) || bid.getAmount() <= 0) {
            throw new InvalidBidException("Bid amount must be a positive number");
        }
        if (state == AuctionState.FINISHED || state == AuctionState.PAID || state == AuctionState.CANCELED) {
            throw new AuctionClosedException("Auction is closed");
        }
        if (state != AuctionState.RUNNING) {
            throw new IllegalStateException("Auction is not running");
        }
        if (now.isBefore(startTime)) {
            throw new IllegalStateException("Auction has not started");
        }
        if (!now.isBefore(endTime)) {
            throw new AuctionClosedException("Auction already ended");
        }
        if (bid.getAmount() <= currentPrice) {
            throw new InvalidBidException("Bid must be higher than current price");
        }
    }
}
