package com.secure.notes.services;

import com.secure.notes.model.RefreshToken;
import jakarta.transaction.Transactional;

import java.util.Optional;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(Long userId);

    RefreshToken verifyExpiration(RefreshToken token);

    Optional<RefreshToken> findByToken(String token);

    void deleteByUserId(Long userId);

    void revokeToken(String token);
}
