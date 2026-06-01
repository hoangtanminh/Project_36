package com.auction.model;

public class Admin extends User {

  public Admin(String id, String name) {
    super(id, name);
  }

  public Admin(String id, String name, String password) {
    super(id, name, password);
  }

  public Admin(String id, String name, String password, double balance) {
    super(id, name, password, balance);
  }
}
