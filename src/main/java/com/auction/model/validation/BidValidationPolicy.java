package com.auction.model.validation;

import java.time.LocalDateTime;

import com.auction.model.AuctionState;
import com.auction.model.Bid;

public interface BidValidationPolicy {
    void validate(
            AuctionState state,
            LocalDateTime now,
            LocalDateTime startTime,
            LocalDateTime endTime,
            double currentPrice,
            Bid bid
    );
}
