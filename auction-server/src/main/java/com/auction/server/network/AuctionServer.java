package com.auction.server.network;

import com.auction.server.event.AuctionEventPublisher;
import com.auction.server.service.AuthenticationService;
import com.auction.server.service.AuctionService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AuctionServer implements AutoCloseable {

    private final int serverPort;
    private final AuthenticationService authService;
    private final AuctionService auctionService;
    private final AuctionEventPublisher publisher;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    private volatile boolean active;
    private ServerSocket listener;

    public AuctionServer(
            int serverPort,
            AuthenticationService authService,
            AuctionService auctionService,
            AuctionEventPublisher publisher) {

        this.serverPort = serverPort;
        this.authService = authService;
        this.auctionService = auctionService;
        this.publisher = publisher;
    }

    public void start() throws IOException {
        listener = new ServerSocket(serverPort);
        active = true;

        System.out.printf("Auction server started on port %d%n", serverPort);

        while (active) {
            waitForClient();
        }
    }

    private void waitForClient() throws IOException {
        try {
            Socket client = listener.accept();

            ClientSession session = new ClientSession(
                    client,
                    authService,
                    auctionService,
                    publisher
            );

            executor.execute(session);

        } catch (SocketException ex) {
            if (active) {
                throw ex;
            }
        }
    }

    @Override
    public void close() throws IOException {
        active = false;

        executor.shutdownNow();

        if (listener != null && !listener.isClosed()) {
            listener.close();
        }
    }
}