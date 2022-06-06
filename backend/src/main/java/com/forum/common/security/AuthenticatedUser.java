package com.forum.common.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Principal carried by the security context for the duration of a request.
 */
@Getter
@AllArgsConstructor
public class AuthenticatedUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String username;
    private final List<String> authorities;

    public boolean hasAuthority(String authority) {
        return authorities.contains(authority);
    }

    public boolean hasAnyAuthority(List<String> candidates) {
        return Collections.disjoint(new java.util.HashSet<>(authorities), candidates);
    }
}
