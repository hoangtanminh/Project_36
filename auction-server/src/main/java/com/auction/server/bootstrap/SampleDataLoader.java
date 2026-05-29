package com.auction.server.bootstrap;

import com.auction.model.*;
import com.auction.server.service.AuctionService;
import com.auction.shared.protocol.AuctionActionRequest;
import com.auction.shared.protocol.BidRequest;

import java.time.LocalDateTime;

public final class SampleDataLoader {

    private final AuctionService service;

    public SampleDataLoader(AuctionService service) {
        this.service = service;
    }

    public void load() {
        initializeUsers();
        initializeAuctions();
    }

    private void initializeUsers() {
        service.seedUser(new Seller("seller01", "Luna Store"));
        service.seedUser(new Bidder("bidder01", "Mia Tran"));
        service.seedUser(new Bidder("bidder02", "Quang Le"));
        service.seedUser(new Admin("admin01", "Ops Desk"));
    }

    private void initializeAuctions() {
        createCameraAuction();
        createHeadphoneAuction();
        createFinishedLaptopAuction();
        createHoodieAuction();
        createGamingLaptopAuction();
    }

    private void createCameraAuction() {
        Item item = new Electronics(
                "I2001",
                "Sony Alpha A7 IV",
                "Mirrorless camera kit",
                1250.0,
                18
        );

        Auction auction = new Auction(
                "A1001",
                item,
                LocalDateTime.now().minusMinutes(10),
                LocalDateTime.now().plusMinutes(90)
        );

        service.seedAuction(auction, new Seller("seller01", "Luna Store"));
        service.placeBid(new BidRequest("A1001", "bidder01", 1325.0));
    }

    private void createHeadphoneAuction() {
        Item item = new Electronics(
                "I2002",
                "Bose QC Ultra",
                "Noise canceling headphones",
                350.0,
                12
        );

        Auction auction = new Auction(
                "A1002",
                item,
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(60)
        );

        service.seedAuction(auction, new Seller("seller01", "Luna Store"));
    }

    private void createFinishedLaptopAuction() {
        Item item = new Electronics(
                "I2003",
                "Framework Laptop 16",
                "DIY Edition with upgraded modules",
                1700.0,
                24
        );

        Auction auction = new Auction(
                "A1003",
                item,
                LocalDateTime.now().minusMinutes(70),
                LocalDateTime.now().plusMinutes(15)
        );

        service.seedAuction(auction, new Seller("seller01", "Luna Store"));
        service.placeBid(new BidRequest("A1003", "bidder02", 1860.0));
        service.finishAuction(new AuctionActionRequest("A1003", "admin01"));
    }

    private void createHoodieAuction() {
        Item item = new Clothing(
                "I2004",
                "Oversized Cotton Hoodie",
                "Streetwear hoodie in washed black",
                65.0,
                "L"
        );

        Auction auction = new Auction(
                "A1004",
                item,
                LocalDateTime.now().minusMinutes(2),
                LocalDateTime.now().plusMinutes(80)
        );

        service.seedAuction(auction, new Seller("seller01", "Luna Store"));
        service.placeBid(new BidRequest("A1004", "bidder02", 79.0));
    }

    private void createGamingLaptopAuction() {
        Item item = new Electronics(
                "I2005",
                "ASUS ROG Zephyrus G16",
                "Gaming laptop with RTX graphics",
                1450.0,
                24
        );

        Auction auction = new Auction(
                "A1005",
                item,
                LocalDateTime.now().minusMinutes(4),
                LocalDateTime.now().plusMinutes(110)
        );

        service.seedAuction(auction, new Seller("seller01", "Luna Store"));
        service.placeBid(new BidRequest("A1005", "bidder01", 1535.0));
    }
}