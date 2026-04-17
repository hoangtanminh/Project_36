package com.auction.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.auction.model.ItemType.Item;

public class Auction {
    private final Item item;
    private Bid highestBid;
    private AuctionState state;

    private final LocalDateTime startTime;
    private LocalDateTime endTime;

    private ScheduledExecutorService scheduler;
    private final ReentrantLock bidLock = new ReentrantLock();
    private final List<AuctionObserver> observers = new CopyOnWriteArrayList<>();

    public Auction(Item item, LocalDateTime startTime, LocalDateTime endTime) {
        if (item == null) {
            throw new IllegalArgumentException("Item is required");
        }
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Invalid auction time range");
        }

        this.item = item;
        this.startTime = startTime;
        this.endTime = endTime;
        this.state = AuctionState.OPEN;
    }

    public synchronized void addObserver(AuctionObserver observer) {
        if (observer != null) {
            observers.add(observer);
        }
    }

    public synchronized void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

    public void startAuction() {
        bidLock.lock();
        try {
            if (state != AuctionState.OPEN) {
                throw new IllegalStateException("Only OPEN auction can move to RUNNING");
            }
            if (LocalDateTime.now().isBefore(startTime)) {
                throw new IllegalStateException("Cannot start auction before start time");
            }
            state = AuctionState.RUNNING;
            startAutoClose();
        } finally {
            bidLock.unlock();
        }
    }

    public void placeBid(Bid bid) {
        bidLock.lock();
        try {
            validateBid(bid);

            long secondsLeft = Duration.between(LocalDateTime.now(), endTime).getSeconds();
            if (secondsLeft <= 10) {
                endTime = endTime.plusSeconds(30);
                restartScheduler();
                System.out.println("Auction extended by 30 seconds!");
            }

            item.setCurrentPrice(bid.getAmount());
            highestBid = bid;

            System.out.println("New highest bid: " + bid.getAmount()
                    + " by " + bid.getBidder().getName());
        } finally {
            bidLock.unlock();
        }

        notifyNewBid(bid);
    }

    private void validateBid(Bid bid) {
        if (bid == null || bid.getBidder() == null) {
            throw new IllegalArgumentException("Bid and bidder are required");
        }
        if (state != AuctionState.RUNNING) {
            throw new IllegalStateException("Auction is not running");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startTime)) {
            throw new IllegalStateException("Auction has not started");
        }
        if (!now.isBefore(endTime)) {
            throw new IllegalStateException("Auction already ended");
        }
        if (bid.getAmount() <= item.getCurrentPrice()) {
            throw new IllegalArgumentException("Invalid bid: must be higher than current price");
        }
    }

    private synchronized void notifyNewBid(Bid bid) {
        for (AuctionObserver observer : observers) {
            observer.onNewBid(this, bid);
        }
    }

    private void startAutoClose() {
        scheduler = Executors.newSingleThreadScheduledExecutor();

        long delay = Duration.between(LocalDateTime.now(), endTime).toMillis();

        if (delay <= 0) {
            finishAuction();
            return;
        }
        scheduler.schedule(() -> {
            finishAuction();
            System.out.println("Auction closed automatically!");
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void restartScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        startAutoClose();
    }

    public void finishAuction() {
        bidLock.lock();
        try {
            if (state != AuctionState.RUNNING) {
                return;
            }
            state = AuctionState.FINISHED;

            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdown();
            }
        } finally {
            bidLock.unlock();
        }
    }

    public void payAuction() {
        bidLock.lock();
        try {
            if (state != AuctionState.FINISHED) {
                throw new IllegalStateException("Only FINISHED auction can move to PAID");
            }
            state = AuctionState.PAID;
        } finally {
            bidLock.unlock();
        }
    }

    public void cancelAuction() {
        bidLock.lock();
        try {
            if (state == AuctionState.PAID) {
                throw new IllegalStateException("PAID auction cannot be canceled");
            }
            if (state == AuctionState.CANCELED) {
                return;
            }
            state = AuctionState.CANCELED;
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdownNow();
            }
        } finally {
            bidLock.unlock();
        }
    }

    public synchronized AuctionState getState() {
        return state;
    }

    public synchronized Bid getHighestBid() {
        return highestBid;
    }

    public Item getItem() {
        return item;
    }

    @Deprecated
    public boolean isOpen() {
        return state == AuctionState.RUNNING;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }
}
