package com.auction.shared.dto;

import com.auction.model.AuctionStatus;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public record AuctionView(
        String auctionId,
        String sellerId,
        String sellerName,
        AuctionStatus status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        ItemView item,
        BidView highestBid,
        List<BidTransactionView> bidHistory,
        String winnerName) implements Serializable {

    public String getAuctionId() {
        return auctionId;
    }

    public String getSellerId() {
        return sellerId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public ItemView getItem() {
        return item;
    }

    public BidView getHighestBid() {
        return highestBid;
    }

    public List<BidTransactionView> getBidHistory() {
        return bidHistory;
    }

    public String getWinnerName() {
        return winnerName;
    }
}
