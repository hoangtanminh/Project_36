package com.auction.model;

public abstract class User {
    protected String id;
    protected String name;
    protected String password;
    protected double balance;
    public User(String id, String name) {
        this.id = id;
        this.name = name;
        this.password = null;
        this.balance = 0.0;
    }

    public User(String id, String name, String password) {
        this.id = id;
        this.name = name;
        this.password = password;
    }
    public User(String id, String name, String password, double balance) {
        this.id = id;
        this.name = name;
        this.password = password == null || password.isBlank() ? id : password;
        this.balance = Math.max(balance, 0.0d);
    }
    public String getName() {
        return name;
    }
    public String getId() {
        return id;
    }

    public String getPassword() {
        return password;
    }
        public synchronized double getBalance() {
        return balance;
    }

    public synchronized void depositFunds(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than zero.");
        }
        balance += amount;
    }

    public synchronized void withdrawFunds(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }
        if (balance < amount) {
            throw new IllegalStateException("Insufficient balance.");
        }
        balance -= amount;
    }
}