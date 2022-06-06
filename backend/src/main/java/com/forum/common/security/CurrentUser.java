package com.forum.common.security;

import com.forum.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Convenience accessor for the authenticated principal of the current thread.
 *
 * <p>Services call {@link #requireUserId()} when they need an owner id; the
 * security filter guarantees an {@link AuthenticatedUser} is present for
 * authenticated endpoints, so a missing principal is a server side wiring bug.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Optional<AuthenticatedUser> find() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        return principal instanceof AuthenticatedUser
                ? Optional.of((AuthenticatedUser) principal)
                : Optional.empty();
    }

    public static Optional<Long> findUserId() {
        return find().map(AuthenticatedUser::getUserId);
    }

    /** @throws BusinessException when the request is not authenticated. */
    public static Long requireUserId() {
        return findUserId().orElseThrow(() -> BusinessException.unauthorized("not logged in"));
    }

    /** @throws BusinessException when the request is not authenticated. */
    public static AuthenticatedUser require() {
        return find().orElseThrow(() -> BusinessException.unauthorized("not logged in"));
    }
}
