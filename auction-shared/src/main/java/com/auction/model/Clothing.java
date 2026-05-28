package com.auction.model;

public class Clothing extends Item {
    private String sizeLabel;

    public Clothing(String id, String name, String description, double startingPrice, String sizeLabel) {
        super(id, name, description, startingPrice);
        this.sizeLabel = sizeLabel;
    }

    public String getSizeLabel() {
        return sizeLabel;
    }

    @Override
    public void printInfo() {
        System.out.println("Clothing: " + getName()
                + " | Price: " + getCurrentPrice()
                + " | Size: " + sizeLabel);
    }

    @Override
    public ItemType getItemType() {
        return ItemType.CLOTHING;
    }
}
