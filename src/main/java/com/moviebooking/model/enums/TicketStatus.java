package com.moviebooking.model.enums;

public enum TicketStatus {
    VALID,       // Vé có hiệu lực (Legacy)
    USED,        // Vé đã được check-in tại cửa rạp (Legacy)
    CANCELLED,   // Vé đã bị hủy/hoàn tiền
    ISSUED,      // Vé mới phát hành
    CHECKED_IN   // Vé đã check-in
}
