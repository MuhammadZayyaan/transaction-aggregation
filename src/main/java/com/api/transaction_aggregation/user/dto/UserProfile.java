package com.api.transaction_aggregation.user.dto;

public record UserProfile(
        Long userId,
        String firstName,
        String lastName,
        String country,
        String idType,
        String idReference,
        String role
) {

    public UserProfile(Long userId, String firstName, String lastName, String country, String idType, String idReference) {
        this(userId, firstName, lastName, country, idType, idReference, null);
    }

    public UserProfile withRole(String role) {
        return new UserProfile(userId, firstName, lastName, country, idType, idReference, role);
    }
}
