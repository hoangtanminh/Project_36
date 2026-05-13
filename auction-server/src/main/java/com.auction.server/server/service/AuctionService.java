package com.auction.server.service;

import com.auction.model.Admin;
import com.auction.model.Art;
import com.auction.model.Auction;
import com.auction.model.AuctionStatus;
import com.auction.model.Bid;
import com.auction.model.Bidder;
import com.auction.model.Clothing;
import com.auction.model.Electronics;
import com.auction.model.Item;
import com.auction.model.ItemFactory;
import com.auction.model.ItemType;
import com.auction.model.Seller;
import com.auction.model.User;
import com.auction.server.dao.AuctionDao;
import com.auction.server.dao.UserDao;
import com.auction.server.domain.ManagedAuction;
import com.auction.server.event.AuctionEventPublisher;
import com.auction.server.mapper.AuctionViewMapper;
import com.auction.shared.dto.AuctionView;
import com.auction.shared.dto.DashboardView;
import com.auction.shared.enums.AuctionEventType;
import com.auction.shared.protocol.AuctionActionRequest;
import com.auction.shared.protocol.BidRequest;
import com.auction.shared.protocol.CreateAuctionRequest;
import com.auction.shared.protocol.ServerResponse;
import com.auction.shared.protocol.UpdateAuctionRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public final class AuctionService {
    private final AuctionDao auctionDao;
    private final UserDao userDao;
    private final AuctionViewMapper mapper;
    private final AuctionEventPublisher eventPublisher;
    private final AtomicInteger auctionSequence = new AtomicInteger(1000);
    private final AtomicInteger itemSequence = new AtomicInteger(2000);
    private final ConcurrentHashMap<String, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();

    public AuctionService(
            AuctionDao auctionDao,
            UserDao userDao,
            AuctionViewMapper mapper,
            AuctionEventPublisher eventPublisher) {
        this.auctionDao = auctionDao;
        this.userDao = userDao;
        this.mapper = mapper;
        this.eventPublisher = eventPublisher;
    }

    public void seedUser(User user) {
        userDao.save(user);
    }

    public void seedAuction(Auction auction, Seller seller) {
        ManagedAuction record = new ManagedAuction(auction, seller, LocalDateTime.now());
        attachObserver(record);
        auctionDao.save(record);
        syncSequences(record);
    }

    public DashboardView loadDashboard(String userId) {
        User user = requireUser(userId);
        List<ManagedAuction> visibleAuctions = auctionDao.findVisibleAuctions();
        List<ManagedAuction> sellerAuctions = user instanceof Admin
                ? auctionDao.findAll()
                : auctionDao.findBySellerId(user.getId());
        return mapper.toDashboard(user, visibleAuctions, sellerAuctions);
    }

    public AuctionView loadAuction(String auctionId) {
        return mapper.toView(requireAuctionRecord(auctionId));
    }

    public AuctionView createAuction(CreateAuctionRequest request) {
        Seller seller = requireSeller(request.sellerId());
        validateCreateOrUpdate(request.startingPrice(), request.durationMinutes(), request.itemName());

        Item item = buildItem(
                request.itemType(),
                nextItemId(),
                request.itemName(),
                request.description(),
                request.startingPrice(),
                request.extraValue());
        Auction auction = new Auction(nextAuctionId(), item, LocalDateTime.now(), LocalDateTime.now().plusMinutes(request.durationMinutes()));
        ManagedAuction record = new ManagedAuction(auction, seller, LocalDateTime.now());
        attachObserver(record);
        auctionDao.save(record);

        AuctionView view = mapper.toView(record);
        publishGlobal(AuctionEventType.AUCTION_CREATED, "Auction created: " + item.getName(), view);
        return view;
    }

    public AuctionView updateAuction(UpdateAuctionRequest request) {
        Seller seller = requireSeller(request.sellerId());
        ManagedAuction record = requireAuctionRecord(request.auctionId());
        requireOwner(record, seller);
        if (record.getAuction().getStatus() != AuctionStatus.OPEN) {
            throw new IllegalStateException("Only OPEN auctions can be updated.");
        }
        validateCreateOrUpdate(request.startingPrice(), request.durationMinutes(), request.itemName());
        record.getAuction().shutdownScheduler();

        Item newItem = buildItem(
                request.itemType(),
                record.getAuction().getItem().getId(),
                request.itemName(),
                request.description(),
                request.startingPrice(),
                request.extraValue());
        Auction replacement = new Auction(
                record.getAuctionId(),
                newItem,
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(request.durationMinutes()));
        record.replaceAuction(replacement);
        attachObserver(record);
        auctionDao.save(record);

        AuctionView view = mapper.toView(record);
        publishGlobal(AuctionEventType.AUCTION_UPDATED, "Auction updated: " + newItem.getName(), view);
        return view;
    }

    public void deleteAuction(AuctionActionRequest request) {
        ManagedAuction record = requireAuctionRecord(request.auctionId());
        User actor = requireUser(request.actorId());
        requireOwnerOrAdmin(record, actor);
        if (record.getAuction().getStatus() != AuctionStatus.OPEN) {
            throw new IllegalStateException("Only OPEN auctions can be deleted.");
        }
        AuctionView deletedView = mapper.toView(record);
        record.getAuction().shutdownScheduler();
        auctionDao.deleteById(record.getAuctionId());
        publishGlobal(AuctionEventType.AUCTION_DELETED, "Auction deleted: " + deletedView.item().name(), deletedView);
    }

    public AuctionView placeBid(BidRequest request) {
        User user = requireUser(request.bidderId());
        if (!(user instanceof Bidder bidder)) {
            throw new IllegalArgumentException("Only bidder accounts can place bids.");
        }

        ManagedAuction record = requireAuctionRecord(request.auctionId());
        ReentrantLock lock = auctionLocks.computeIfAbsent(record.getAuctionId(), ignored -> new ReentrantLock());
        lock.lock();
        try {
            record.getAuction().placeBid(new Bid(bidder, request.amount()));
            return mapper.toView(record);
        } finally {
            lock.unlock();
        }
    }

    public AuctionView finishAuction(AuctionActionRequest request) {
        ManagedAuction record = requireAuctionRecord(request.auctionId());
        User actor = requireUser(request.actorId());
        requireOwnerOrAdmin(record, actor);
        record.getAuction().closeAuction();
        return mapper.toView(record);
    }

    public AuctionView markPaid(AuctionActionRequest request) {
        ManagedAuction record = requireAuctionRecord(request.auctionId());
        User actor = requireUser(request.actorId());
        requireAdmin(actor);
        if (record.getAuction().getStatus() != AuctionStatus.FINISHED) {
            throw new IllegalStateException("Only FINISHED auctions can be marked paid.");
        }
        record.getAuction().markPaid();
        return mapper.toView(record);
    }

    public AuctionView cancelAuction(AuctionActionRequest request) {
        ManagedAuction record = requireAuctionRecord(request.auctionId());
        User actor = requireUser(request.actorId());
        requireAdmin(actor);
        if (record.getAuction().getStatus() == AuctionStatus.PAID) {
            throw new IllegalStateException("PAID auctions cannot be canceled.");
        }
        record.getAuction().cancelAuction();
        return mapper.toView(record);
    }

    // Dung tat ca timer auction de server co the shutdown sach ma khong de lai thread nen.
    public void shutdown() {
        auctionDao.findAll().forEach(record -> record.getAuction().shutdownScheduler());
    }

    private void attachObserver(ManagedAuction record) {
        record.getAuction().addObserver(new AuctionObserverBridge(
                record.getAuctionId(),
                record.getAuction(),
                auctionDao,
                mapper,
                eventPublisher));
    }

    private void publishGlobal(AuctionEventType type, String message, AuctionView view) {
        ServerResponse<AuctionView> response = ServerResponse.event(type, message, view);
        eventPublisher.publishGlobal(response);
    }

    private Item buildItem(String itemTypeValue, String itemId, String name, String description, double startingPrice, String extraValue) {
        ItemType itemType = parseItemType(itemTypeValue);
        return switch (itemType) {
            case ART -> ItemFactory.createItem(itemType, itemId, name, description, startingPrice, blankToDefault(extraValue, "Unknown Artist"));
            case ELECTRONICS -> ItemFactory.createItem(itemType, itemId, name, description, startingPrice, parseWarranty(extraValue));
            case CLOTHING -> ItemFactory.createItem(itemType, itemId, name, description, startingPrice, blankToDefault(extraValue, "M"));
        };
    }

    private ItemType parseItemType(String itemTypeValue) {
        if (itemTypeValue == null || itemTypeValue.isBlank()) {
            return ItemType.ELECTRONICS;
        }
        try {
            return ItemType.valueOf(itemTypeValue.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported item type: " + itemTypeValue);
        }
    }

    private int parseWarranty(String extraValue) {
        if (extraValue == null || extraValue.isBlank()) {
            return 12;
        }
        try {
            return Integer.parseInt(extraValue.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Warranty must be a whole number of months.");
        }
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private void validateCreateOrUpdate(double startingPrice, int durationMinutes, String itemName) {
        if (itemName == null || itemName.isBlank()) {
            throw new IllegalArgumentException("Item name is required.");
        }
        if (startingPrice <= 0) {
            throw new IllegalArgumentException("Starting price must be greater than zero.");
        }
        if (durationMinutes < 1) {
            throw new IllegalArgumentException("Duration must be at least 1 minute.");
        }
    }

    private ManagedAuction requireAuctionRecord(String auctionId) {
        return auctionDao.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Auction not found: " + auctionId));
    }

    private User requireUser(String userId) {
        return userDao.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown user: " + userId));
    }

    private Seller requireSeller(String sellerId) {
        User user = requireUser(sellerId);
        if (!(user instanceof Seller seller)) {
            throw new IllegalArgumentException("Only seller accounts can manage auctions.");
        }
        return seller;
    }

    private void requireOwner(ManagedAuction record, Seller seller) {
        if (!record.getSeller().getId().equals(seller.getId())) {
            throw new IllegalArgumentException("You can only manage your own auctions.");
        }
    }

    private void requireOwnerOrAdmin(ManagedAuction record, User actor) {
        if (actor instanceof Admin) {
            return;
        }
        if (!(actor instanceof Seller seller)) {
            throw new IllegalArgumentException("Only the seller owner or admin can perform this action.");
        }
        requireOwner(record, seller);
    }

    private void requireAdmin(User actor) {
        if (!(actor instanceof Admin)) {
            throw new IllegalArgumentException("Only admin accounts can perform this action.");
        }
    }

    private void syncSequences(ManagedAuction record) {
        auctionSequence.set(Math.max(auctionSequence.get(), extractNumericSuffix(record.getAuctionId(), 1000)));
        itemSequence.set(Math.max(itemSequence.get(), extractNumericSuffix(record.getAuction().getItem().getId(), 2000)));
    }

    private int extractNumericSuffix(String id, int fallback) {
        if (id == null || id.length() < 2) {
            return fallback;
        }
        try {
            return Integer.parseInt(id.replaceAll("\\D+", ""));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private String nextAuctionId() {
        return "A" + auctionSequence.incrementAndGet();
    }

    private String nextItemId() {
        return "I" + itemSequence.incrementAndGet();
    }
}
