package com.auction.model.ItemType;

public abstract class Item {
    private final String name;
    private double currentPrice;

    public Item(String name, double price) {
        this.name = name;
        this.currentPrice = price;
    }

    public String getName() {
        return name;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double price) {
        this.currentPrice = price;
    }
}
