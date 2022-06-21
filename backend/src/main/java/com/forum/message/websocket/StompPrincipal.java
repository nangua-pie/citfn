package com.forum.message.websocket;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.security.Principal;

/**
 * Authenticated STOMP session principal.
 *
 * <p>Spring derives the {@code /user/**} destination from
 * {@link Principal#getName()}, so the name must be stable and unique per user.
 */
@Getter
@AllArgsConstructor
public class StompPrincipal implements Principal {

    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String username;

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
