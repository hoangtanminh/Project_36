package com.auction.server.app;

import com.auction.server.bootstrap.SampleDataLoader;
import com.auction.server.dao.AuctionDao;
import com.auction.server.dao.UserDao;
import com.auction.server.dao.memory.InMemoryAuctionDao;
import com.auction.server.dao.memory.InMemoryUserDao;
import com.auction.server.event.AuctionEventPublisher;
import com.auction.server.mapper.AuctionViewMapper;
import com.auction.server.network.AuctionServer;
import com.auction.server.service.AuthenticationService;
import com.auction.server.service.AuctionService;

public final class AuctionServerApplication {
    private AuctionServerApplication() {
    }

    public static void main(String[] args) throws Exception {
        AuctionDao auctionDao = new InMemoryAuctionDao();
        UserDao userDao = new InMemoryUserDao();
        AuctionEventPublisher eventPublisher = new AuctionEventPublisher();
        AuctionViewMapper mapper = new AuctionViewMapper();
        AuctionService auctionService = new AuctionService(auctionDao, userDao, mapper, eventPublisher);
        new SampleDataLoader(auctionService).load();
        AuthenticationService authenticationService = new AuthenticationService(userDao);

        try (AuctionServer server = new AuctionServer(5050, authenticationService, auctionService, eventPublisher)) {
            server.start();
        }
    }
}
