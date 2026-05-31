package com.auction.model;

/**
 * Represents a generic user in the auction system.
 */
public abstract class User {
  protected String id;
  protected String name;
  protected String password;
  protected double balance;

  /**
   * Constructs a User with an ID and name.
   *
   * @param id The user ID
   * @param name The user's name
   */
  public User(String id, String name) {
    this.id = id;
    this.name = name;
    this.password = null;
    this.balance = 0.0;
  }

  /**
   * Constructs a User with ID, name, and password.
   *
   * @param id The user ID
   * @param name The user's name
   * @param password The user's password
   */
  public User(String id, String name, String password) {
    this.id = id;
    this.name = name;
    this.password = password;
  }

  /**
   * Constructs a User with all details.
   *
   * @param id The user ID
   * @param name The user's name
   * @param password The user's password
   * @param balance The user's balance
   */
  public User(String id, String name, String password, double balance) {
    this.id = id;
    this.name = name;
    this.password = password == null || password.isBlank() ? id : password;
    this.balance = Math.max(balance, 0.0d);
  }

  /**
   * Gets the user's name.
   *
   * @return The name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the user's ID.
   *
   * @return The ID
   */
  public String getId() {
    return id;
  }

  /**
   * Gets the user's password.
   *
   * @return The password
   */
  public String getPassword() {
    return password;
  }

  /**
   * Gets the user's balance.
   *
   * @return The balance
   */
  public synchronized double getBalance() {
    return balance;
  }

  /**
   * Deposits funds into the user's balance.
   *
   * @param amount The amount to deposit
   */
  public synchronized void depositFunds(double amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException("Deposit amount must be greater than zero.");
    }
    balance += amount;
  }

  /**
   * Withdraws funds from the user's balance.
   *
   * @param amount The amount to withdraw
   */
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