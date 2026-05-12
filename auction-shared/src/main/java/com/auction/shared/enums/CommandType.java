package com.auction.shared.enums;

import java.io.Serializable;

public enum CommandType implements Serializable {
    LOGIN,
    REGISTER,
    LOAD_DASHBOARD,
    LOAD_AUCTION_DETAILS,
    SUBSCRIBE_AUCTION,
    PLACE_BID,
    CREATE_AUCTION,
    UPDATE_AUCTION,
    DELETE_AUCTION,
    FINISH_AUCTION,
    MARK_PAID,
    CANCEL_AUCTION,
    LOGOUT
}
