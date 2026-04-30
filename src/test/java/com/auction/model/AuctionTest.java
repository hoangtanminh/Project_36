package com.auction.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuctionTest {

    @Test
    void placeBid_lowerThanCurrentPrice() {
        Item item = new Art("A01", "Mona Lisa", "Painting", 1_000.0, "Leonardo da Vinci");
        Auction auction = new Auction(
                item,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusMinutes(5)
        );
        Bidder bidder = new Bidder("B01", "Minh");
        Bid lowBid = new Bid(bidder, 900.0);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> auction.placeBid(lowBid));

        assertEquals("Invalid bid: must be higher than current price", exception.getMessage());
        assertEquals(1_000.0, item.getCurrentPrice());
        assertNull(auction.getHighestBid());

        auction.closeAuction();
    }
}
