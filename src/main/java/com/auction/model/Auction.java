package com.auction.model;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.auction.model.ItemType.Item;
import com.auction.model.validation.BidValidationPolicy;
import com.auction.model.validation.DefaultBidValidationPolicy;

public class Auction {
    private final Item item;
    private volatile Bid highestBid;
    private volatile AuctionState state;

    private final LocalDateTime startTime;
    private volatile LocalDateTime endTime;

    private ScheduledExecutorService scheduler;
    private final ReentrantLock bidLock = new ReentrantLock();
    private final List<AuctionObserver> observers = new CopyOnWriteArrayList<>();

    private final BidValidationPolicy bidValidationPolicy;
    private final Clock clock;

    public Auction(Item item, LocalDateTime startTime, LocalDateTime endTime) {
        this(item, startTime, endTime, new DefaultBidValidationPolicy(), Clock.systemDefaultZone());
    }

    public Auction(
            Item item,
            LocalDateTime startTime,
            LocalDateTime endTime,
            BidValidationPolicy bidValidationPolicy,
            Clock clock
    ) {
        if (item == null) {
            throw new IllegalArgumentException("Item is required");
        }
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Invalid auction time range");
        }
        if (bidValidationPolicy == null) {
            throw new IllegalArgumentException("BidValidationPolicy is required");
        }
        if (clock == null) {
            throw new IllegalArgumentException("Clock is required");
        }

        this.item = item;
        this.startTime = startTime;
        this.endTime = endTime;
        this.state = AuctionState.OPEN;
        this.bidValidationPolicy = bidValidationPolicy;
        this.clock = clock;
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
            if (now().isBefore(startTime)) {
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
            bidValidationPolicy.validate(state, now(), startTime, endTime, item.getCurrentPrice(), bid);

            long secondsLeft = Duration.between(now(), endTime).getSeconds();
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

    private synchronized void notifyNewBid(Bid bid) {
        for (AuctionObserver observer : observers) {
            observer.onNewBid(this, bid);
        }
    }

    private void startAutoClose() {
        scheduler = Executors.newSingleThreadScheduledExecutor();

        long delay = Duration.between(now(), endTime).toMillis();

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

    public AuctionState getState() {
        return state;
    }

    public Bid getHighestBid() {
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

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
