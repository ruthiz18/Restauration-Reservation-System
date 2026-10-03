package com.reservo.security;

import com.reservo.domain.Role;

/** Lightweight principal rebuilt from JWT claims on every request - no DB hit needed to authenticate. */
public record AuthUser(Long id, String email, Role role) {
    public boolean isStaffOrAdmin() {
        return role == Role.STAFF || role == Role.ADMIN;
    }
}
