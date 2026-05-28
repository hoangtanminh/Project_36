package com.auction.model;

public abstract class User {
    protected String id;
    protected String name;
    protected String password;

    public User(String id, String name) {
        this.id = id;
        this.name = name;
        this.password = null;
    }

    public User(String id, String name, String password) {
        this.id = id;
        this.name = name;
        this.password = password;
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
}