package com.auction.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuctionTest {

    @Test
    void placeBid_lowerThanCurrentPrice() {
        Item item = new Art("A01", "Mona Lisa", "Painting", 1_000.0, "Leonardo da Vinci");
        Auction auction = new Auction(
                "A01",
                item,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusMinutes(5)
        );
        Bidder bidder = new Bidder("B01", "Minh");
        Bid lowBid = new Bid(bidder, 900.0);
        RuntimeException exception = null;
        try{
            auction.placeBid(lowBid);
        } catch (RuntimeException e) {
            exception = e;
        }
        
        assertEquals("Bid must be higher than current price: " + item.getCurrentPrice(), exception.getMessage());
        assertEquals(1_000.0, item.getCurrentPrice());
        assertNull(auction.getHighestBid());

        auction.closeAuction();
    }
}
