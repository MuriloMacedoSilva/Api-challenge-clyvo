package com.FirstApiChallenge.api.service;

public record ExpoPushTicket(boolean accepted, String errorCode, String message) {

    public static ExpoPushTicket success() {
        return new ExpoPushTicket(true, null, null);
    }

    public static ExpoPushTicket rejected(String errorCode, String message) {
        return new ExpoPushTicket(false, errorCode, message);
    }

    public boolean deviceNotRegistered() {
        return "DeviceNotRegistered".equals(errorCode);
    }
}
