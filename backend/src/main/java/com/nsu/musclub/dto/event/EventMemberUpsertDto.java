package com.nsu.musclub.dto.event;

import jakarta.validation.constraints.NotNull;

public class EventMemberUpsertDto {
    @NotNull
    private Long userId;

    // The role must be added by the user, and it can be anyone at the request of the organizer.
    // TODO  Dynamic role addition should be added
    @NotNull
    private String role;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
