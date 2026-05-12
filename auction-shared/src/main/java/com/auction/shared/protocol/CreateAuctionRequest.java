package com.auction.shared.protocol;

import java.io.Serializable;

public record CreateAuctionRequest(
        String sellerId,
        String itemType,
        String itemName,
        String description,
        double startingPrice,
        int durationMinutes,
        String extraValue) implements Serializable {

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

    public CreateAuctionRequest withSellerId(String normalizedSellerId) {
        return new CreateAuctionRequest(normalizedSellerId, itemType, itemName, description, startingPrice, durationMinutes, extraValue);
    }
}
