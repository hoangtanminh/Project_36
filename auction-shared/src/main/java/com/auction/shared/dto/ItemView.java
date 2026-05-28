package com.auction.shared.dto;

import java.io.Serializable;

public record ItemView(
        String id,
        String name,
        String description,
        double startingPrice,
        double currentPrice,
        String itemType,
        String detailLabel) implements Serializable {

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public String getItemType() {
        return itemType;
    }

    public String getDetailLabel() {
        return detailLabel;
    }

    // Compatibility aliases used by the client UI
    public String getTitle() { return name; }
    public String getCategory() { return itemType; }
    public String getHighlightLine() { return detailLabel; }
    public String getImageHint() { return ""; }
    public java.math.BigDecimal getOpeningPrice() { return java.math.BigDecimal.valueOf(startingPrice); }
    public java.math.BigDecimal getMinimumNextBid() { return java.math.BigDecimal.valueOf(currentPrice + 1.0); }
}
