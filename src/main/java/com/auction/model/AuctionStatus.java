package com.auction.model;

public enum AuctionStatus {
    OPEN,       // phiên vừa tạo, chưa có bid nào
    RUNNING,    // đang có người đấu giá
    FINISHED,   // hết giờ, xác định người thắng
    PAID,       // người thắng đã thanh toán
    CANCELED    // phiên bị hủy
}