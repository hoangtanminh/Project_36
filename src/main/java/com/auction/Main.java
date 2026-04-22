package com.auction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
import com.auction.model.Auction;
import com.auction.model.Bid;
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
        List<Future<?>> futures = List.of(
                executor.submit(() -> safePlaceBid(auction, new Bid(a, 1100))),
                executor.submit(() -> safePlaceBid(auction, new Bid(b, 1200))),
                executor.submit(() -> safePlaceBid(auction, new Bid(c, 1300)))
        );

        executor.shutdown();
        waitForCompletion(executor, futures);

        auction.finishAuction();
        auction.payAuction();

        System.out.println("Final highest bid: " + auction.getHighestBid().getAmount()
                + " by " + auction.getHighestBid().getBidder().getName());
        System.out.println("Auction state: " + auction.getState());
    }

    private static void safePlaceBid(Auction auction, Bid bid) {
        try {
            auction.placeBid(bid);
        } catch (InvalidBidException | AuctionClosedException | IllegalArgumentException ex) {
            System.out.println("Bid failed: " + ex.getMessage());
        }
    }

    private static void waitForCompletion(ExecutorService executor, List<Future<?>> futures) {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception ex) {
                System.out.println("Unexpected bidding error: " + ex.getMessage());
            }
        }

        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Bidding threads interrupted", ex);
        }
    }
}
