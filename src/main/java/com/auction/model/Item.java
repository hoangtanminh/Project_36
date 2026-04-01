package com.auction.model;

public class Item {
    private String name;
    private double currentPrice;

    public Item(String name, double price) {
        this.name = name;
        this.currentPrice = price;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double price) {
        this.currentPrice = price;
    }
}