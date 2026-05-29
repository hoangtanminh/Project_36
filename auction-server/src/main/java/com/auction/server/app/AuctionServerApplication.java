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

    private static final int SERVER_PORT = 5050;

    private AuctionServerApplication() {
    }

    public static void main(String[] args) throws Exception {
        AuctionDao auctionDao = new InMemoryAuctionDao();
        UserDao userDao = new InMemoryUserDao();

        AuctionEventPublisher publisher = new AuctionEventPublisher();

        AuctionService auctionService =
                buildAuctionService(auctionDao, userDao, publisher);

        AuthenticationService authService =
                buildAuthenticationService(userDao);

        runServer(authService, auctionService, publisher);
    }

    private static AuctionService buildAuctionService(
            AuctionDao auctionDao,
            UserDao userDao,
            AuctionEventPublisher publisher) {

        AuctionViewMapper mapper = new AuctionViewMapper();

        AuctionService service =
                new AuctionService(auctionDao, userDao, mapper, publisher);

        new SampleDataLoader(service).load();

        return service;
    }

    private static AuthenticationService buildAuthenticationService(
            UserDao userDao) {

        return new AuthenticationService(userDao);
    }

    private static void runServer(
            AuthenticationService authService,
            AuctionService auctionService,
            AuctionEventPublisher publisher) throws Exception {

        try (AuctionServer server =
                     new AuctionServer(
                             SERVER_PORT,
                             authService,
                             auctionService,
                             publisher)) {

            Runtime.getRuntime().addShutdownHook(
                    new Thread(
                            auctionService::shutdown,
                            "auction-server-shutdown"));

            server.start();
        } finally {
            auctionService.shutdown();
        }
    }
}