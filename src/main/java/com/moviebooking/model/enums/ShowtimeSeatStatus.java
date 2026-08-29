package com.moviebooking.model.enums;

public enum ShowtimeSeatStatus {
    AVAILABLE, // Ghế còn trống, sẵn sàng cho đặt
    HELD,      // Ghế đang giữ tạm thời (trong 8 phút)
    SOLD       // Ghế đã bán thành công
}
