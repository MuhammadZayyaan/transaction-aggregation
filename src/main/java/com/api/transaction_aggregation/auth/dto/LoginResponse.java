package com.api.transaction_aggregation.auth.dto;

public record LoginResponse(
        boolean success,
        String token,
        String username,
        String message
) {

    public static LoginResponse success(String token, String username) {
        return new LoginResponse(true, token, username, "Login successful");
    }

    public static LoginResponse failed(String message) {
        return new LoginResponse(false, null, null, message);
    }
}
