package com.auction;

import com.auction.model.*;

import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {

        Item item = new Electronics("E01", "Laptop", "Gaming", 1000, 12);

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusSeconds(60);

        Auction auction = new Auction(item, start, end);

        Bidder bidder1 = new Bidder("B01", "User1");
        Bidder bidder2 = new Bidder("B02", "User2");

        Observer ob1 = new BidderObserver("User1");
        Observer ob2 = new BidderObserver("User2");

        auction.addObserver(ob1);
        auction.addObserver(ob2);

        Bid bid1 = new Bid(bidder1, 1200);
        Bid bid2 = new Bid(bidder2, 1500);

        auction.placeBid(bid1);
        auction.placeBid(bid2);
    }
}