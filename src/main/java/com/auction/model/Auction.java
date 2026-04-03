package com.auction.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.auction.model.ItemType.Item;

public class Auction {
    private Item item;
    private Bid highestBid;
    private boolean isOpen;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private ScheduledExecutorService scheduler;

    public Auction(Item item, LocalDateTime startTime, LocalDateTime endTime) {
        this.item = item;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isOpen = true;

        startAutoClose();
    }

    // 🔥 CORE: đặt giá (giữ logic cũ + thêm anti-sniping)
    public synchronized void placeBid(Bid bid) {

        if (!isOpen) {
            throw new RuntimeException("Auction is closed");
        }

        if (bid.getAmount() <= item.getCurrentPrice()) {
            throw new RuntimeException("Invalid bid: must be higher than current price");
        }

        long secondsLeft = Duration.between(LocalDateTime.now(), endTime).getSeconds();
        if (secondsLeft <= 10) {
            endTime = endTime.plusSeconds(30);
            System.out.println("Auction extended by 30 seconds!");

            restartScheduler();
        }

        item.setCurrentPrice(bid.getAmount());
        highestBid = bid;

        System.out.println("New highest bid: " + bid.getAmount()
                + " by " + bid.getBidder().getName());
    }
    private void startAutoClose() {
        scheduler = Executors.newSingleThreadScheduledExecutor();

        long delay = Duration.between(LocalDateTime.now(), endTime).toMillis();

        if (delay <= 0) {
            closeAuction();
            return;
        }
        scheduler.schedule(() -> {
            closeAuction();
            System.out.println("Auction closed automatically!");
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void restartScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        startAutoClose();
    }

    public void closeAuction() {
        isOpen = false;

        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    public Bid getHighestBid() {
        return highestBid;
    }

    public Item getItem() {
        return item;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }
}