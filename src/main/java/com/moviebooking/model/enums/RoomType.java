package com.moviebooking.model.enums;

public enum RoomType {
    TWO_D("2D"),
    THREE_D("3D"),
    FOUR_DX("4DX"),
    IMAX("IMAX"),
    VIP_ROOM("VIP");
    private final String dbValue;
    RoomType(String dbValue) {
        this.dbValue = dbValue;
    }
    public String getDbValue() {
        return dbValue;
    }
    public static RoomType fromDbValue(String dbValue) {
        for (RoomType type : values()) {
            if (type.dbValue.equalsIgnoreCase(dbValue)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown RoomType: " + dbValue);
    }
}
