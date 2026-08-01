package com.moviebooking.model.enums;

public enum ReservationStatus {
    PENDING,   // Đang chờ thanh toán (giữ ghế trong 8 phút)
    CONFIRMED, // Đã thanh toán thành công
    EXPIRED,   // Quá hạn 8 phút chưa thanh toán (tự động giải phóng ghế)
    CANCELLED  // Đã bị hủy
}
