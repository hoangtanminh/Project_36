package com.auction.model;

/**
 * Represents a piece of art item.
 */
public class Art extends Item {
  private String artist;

  public Art(String id, String name, String description,
             double startingPrice, String artist) {
    super(id, name, description, startingPrice);
    this.artist = artist;
  }

  public String getArtist() {
    return artist;
  }

  @Override
  public void printInfo() {
    System.out.println("Art: " + getName()
        + " | Price: " + getCurrentPrice()
        + " | Artist: " + artist);
  }

  @Override
  public ItemType getItemType() {
    return ItemType.ART;
  }
}