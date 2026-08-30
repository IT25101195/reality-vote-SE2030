package com.slit.realityvote.service;

import com.slit.realityvote.entity.User;
import org.springframework.security.core.Authentication;

public interface AuthBridgeService {
    /**
     * Resolves the `users` table row for the currently authenticated
     * principal, auto-provisioning one on first use. See the class
     * comment on AuthBridgeServiceImpl for why this exists.
     */
    User resolveCurrentUser(Authentication authentication);
}
