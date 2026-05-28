package com.auction.server.dao.file;

import com.auction.model.Admin;
import com.auction.model.Bidder;
import com.auction.model.Seller;
import com.auction.model.User;
import com.auction.server.dao.UserDao;
import com.auction.shared.enums.UserRole;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class FileBackedUserDao implements UserDao {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Path storagePath;

    public FileBackedUserDao(Path storagePath) {
        if (storagePath == null) {
            throw new IllegalArgumentException("Storage path is required.");
        }
        this.storagePath = storagePath.toAbsolutePath().normalize();
        loadFromDisk();
    }

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public List<User> findAll() {
        return users.values().stream().toList();
    }

    @Override
    public synchronized void save(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User is required.");
        }
        users.put(user.getId(), user);
        persist();
    }

    private void loadFromDisk() {
        try {
            createParentDirectoryIfNeeded();
            if (!Files.exists(storagePath)) {
                return;
            }

            try (ObjectInputStream inputStream = new ObjectInputStream(Files.newInputStream(storagePath))) {
                readPersistedUsers(inputStream.readObject());
            }
        } catch (IOException | ClassNotFoundException | RuntimeException exception) {
            recoverFromCorruptStorage(exception);
        }
    }

    private void persist() {
        try {
            createParentDirectoryIfNeeded();
            try (ObjectOutputStream outputStream = new ObjectOutputStream(Files.newOutputStream(
                    storagePath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE))) {
                outputStream.writeObject(snapshotUsers());
                outputStream.flush();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save users to " + storagePath, exception);
        }
    }

    private void createParentDirectoryIfNeeded() throws IOException {
        Path parent = storagePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private void readPersistedUsers(Object payload) {
        if (!(payload instanceof List<?> persistedUsers)) {
            throw new IllegalStateException("Unexpected user storage format in " + storagePath);
        }

        for (Object persistedUser : persistedUsers) {
            if (persistedUser instanceof StoredUser storedUser) {
                User user = toDomainUser(storedUser);
                users.put(user.getId(), user);
                continue;
            }
            if (persistedUser instanceof User user) {
                users.put(user.getId(), user);
                continue;
            }
            throw new IllegalStateException("Invalid user entry found in " + storagePath);
        }
    }

    private List<StoredUser> snapshotUsers() {
        List<StoredUser> snapshot = new ArrayList<>();
        for (User user : users.values()) {
            snapshot.add(toStoredUser(user));
        }
        return snapshot;
    }

    private StoredUser toStoredUser(User user) {
        return new StoredUser(
                user.getId(),
                user.getName(),
                user.getPassword(),
                resolveRole(user));
    }

    private User toDomainUser(StoredUser storedUser) {
        UserRole role = UserRole.valueOf(storedUser.role());
        return switch (role) {
            case BIDDER -> new Bidder(storedUser.id(), storedUser.name(), storedUser.password());
            case SELLER -> new Seller(storedUser.id(), storedUser.name(), storedUser.password());
            case ADMIN -> new Admin(storedUser.id(), storedUser.name(), storedUser.password());
        };
    }

    private String resolveRole(User user) {
        if (user instanceof Seller) {
            return UserRole.SELLER.name();
        }
        if (user instanceof Bidder) {
            return UserRole.BIDDER.name();
        }
        if (user instanceof Admin) {
            return UserRole.ADMIN.name();
        }
        throw new IllegalArgumentException("Unsupported user type: " + user.getClass().getName());
    }

    private void recoverFromCorruptStorage(Exception exception) {
        users.clear();
        Path backupPath = backupCorruptStorage(exception);
        System.err.println("Warning: user storage at " + storagePath
                + " was unreadable and has been moved to " + backupPath
                + ". Starting with a fresh user store.");
    }

    private Path backupCorruptStorage(Exception originalException) {
        try {
            Path backupPath = storagePath.resolveSibling(
                    storagePath.getFileName() + ".corrupt-" + System.currentTimeMillis() + ".bak");
            Files.move(storagePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            return backupPath;
        } catch (IOException backupException) {
            IllegalStateException failure = new IllegalStateException("Unable to load users from " + storagePath, originalException);
            failure.addSuppressed(backupException);
            throw failure;
        }
    }

    private record StoredUser(
            String id,
            String name,
            String password,
            String role) implements Serializable {
        private static final long serialVersionUID = 1L;
    }
}
