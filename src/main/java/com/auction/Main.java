package com.auction;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.auction.model.*;
import com.auction.model.ItemType.Electronics;
import com.auction.model.ItemType.Item;
import com.auction.model.User.Bidder;

public class Main {
    public static void main(String[] args) {
        Item item = new Electronics("iPhone", 1000);
        Auction auction = new Auction(item, LocalDateTime.now(), LocalDateTime.now().plusMinutes(1));

        Bidder a = new Bidder("1", "Minh");
        Bidder b = new Bidder("2", "An");
        Bidder c = new Bidder("3", "Linh");

        auction.addObserver((a1, bid) -> System.out.println(
                "[Observer] New bid: " + bid.getAmount() + " by " + bid.getBidder().getName()));

        auction.startAuction();

        ExecutorService executor = Executors.newFixedThreadPool(3);
        executor.submit(() -> auction.placeBid(new Bid(a, 1100)));
        executor.submit(() -> auction.placeBid(new Bid(b, 1200)));
        executor.submit(() -> auction.placeBid(new Bid(c, 1300)));

        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Bidding threads interrupted", e);
        }

        auction.finishAuction();
        auction.payAuction();

        System.out.println("Final highest bid: " + auction.getHighestBid().getAmount());
        System.out.println("Auction state: " + auction.getState());
    }
}
