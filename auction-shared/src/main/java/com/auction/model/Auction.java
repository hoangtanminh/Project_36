package com.auction.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Represents an auction item being bid on.
 */
public class Auction implements Subject {

  private final String auctionId;
  private final Item item;
  private Bid highestBid;
  private AuctionStatus status;

  private final LocalDateTime startTime;
  private volatile LocalDateTime endTime;
  private ScheduledExecutorService scheduler;
  private final List<Observer> observers = new CopyOnWriteArrayList<>();
  private final List<BidTransaction> bidHistory = new ArrayList<>();

  /**
   * Constructs an auction.
   *
   * @param auctionId The auction id.
   * @param item The item.
   * @param startTime The start time.
   * @param endTime The end time.
   */
  public Auction(String auctionId, Item item, LocalDateTime startTime, LocalDateTime endTime) {
    this.auctionId = auctionId;
    this.item = item;
    this.startTime = startTime;
    this.endTime = endTime;
    this.status = AuctionStatus.OPEN;
    startAutoClose();
  }

  /**
   * Places a bid on the auction.
   *
   * @param bid The bid.
   */
  public synchronized void placeBid(Bid bid) {
    if (status != AuctionStatus.OPEN && status != AuctionStatus.RUNNING) {
      throw new IllegalStateException("Auction is not open for bidding");
    }

    if (bid.getAmount() <= item.getCurrentPrice()) {
      throw new IllegalArgumentException("Bid must be higher than current price: "
          + item.getCurrentPrice());
    }

    long secondsLeft = Duration.between(LocalDateTime.now(), endTime).getSeconds();
    if (secondsLeft <= 10) {
      endTime = endTime.plusSeconds(30);
      System.out.println("Auction extended by 30 seconds! New end time: " + endTime);
      restartScheduler();
    }

    item.setCurrentPrice(bid.getAmount());
    highestBid = bid;
    status = AuctionStatus.RUNNING;
    bidHistory.add(new BidTransaction(auctionId, bid));
    notifyObservers();

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

  /**
   * Shutdown the internal scheduler used for auto-closing the auction.
   * Exposed to allow external management of scheduler lifecycle.
   */
  public void shutdownScheduler() {
    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdownNow();
    }
  }

  /**
   * Closes the auction.
   */
  public synchronized void closeAuction() {
    if (status == AuctionStatus.FINISHED || status == AuctionStatus.CANCELED) {
      return;
    }
    status = AuctionStatus.FINISHED;
    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdown();
    }
    notifyObservers();
  }

  /**
   * Cancels the auction.
   */
  public synchronized void cancelAuction() {
    status = AuctionStatus.CANCELED;
    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdown();
    }
    notifyObservers();
  }

  /**
   * Marks the auction as paid.
   */
  public synchronized void markPaid() {
    if (status == AuctionStatus.FINISHED) {
      status = AuctionStatus.PAID;
      notifyObservers();
    }
  }

  @Override
  public void addObserver(Observer observer) {
    observers.add(observer);
  }

  @Override
  public void removeObserver(Observer observer) {
    observers.remove(observer);
  }

  @Override
  public void notifyObservers() {
    for (Observer observer : observers) {
      observer.update(this);
    }
  }

  public String getAuctionId() {
    return auctionId;
  }

  public Bid getHighestBid() {
    return highestBid;
  }

  public Item getItem() {
    return item;
  }

  public AuctionStatus getStatus() {
    return status;
  }

  public boolean isOpen() {
    return status == AuctionStatus.OPEN || status == AuctionStatus.RUNNING;
  }

  public LocalDateTime getStartTime() {
    return startTime;
  }

  public LocalDateTime getEndTime() {
    return endTime;
  }

  public List<BidTransaction> getBidHistory() {
    return Collections.unmodifiableList(bidHistory);
  }
}