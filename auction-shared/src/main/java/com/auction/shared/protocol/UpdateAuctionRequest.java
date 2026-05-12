package com.auction.shared.protocol;

import java.io.Serializable;

public record UpdateAuctionRequest(
        String auctionId,
        String sellerId,
        String itemType,
        String itemName,
        String description,
        double startingPrice,
        int durationMinutes,
        String extraValue) implements Serializable {

    public String getAuctionId() {
        return auctionId;
    }

    public String getSellerId() {
        return sellerId;
    }

    public String getItemType() {
        return itemType;
    }

    public String getItemName() {
        return itemName;
    }

    public String getDescription() {
        return description;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public String getExtraValue() {
        return extraValue;
    }

    public UpdateAuctionRequest withSellerId(String normalizedSellerId) {
        return new UpdateAuctionRequest(auctionId, normalizedSellerId, itemType, itemName, description, startingPrice, durationMinutes, extraValue);
    }
}
