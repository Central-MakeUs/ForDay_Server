package com.example.ForDay.domain.auth.service;

import com.example.ForDay.domain.auth.entity.RefreshToken;
import com.example.ForDay.domain.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public void save(Long userId, String refreshToken) {
        refreshTokenRepository.save(new RefreshToken(userId, refreshToken));
    }

    public String get(String userId) {
        return refreshTokenRepository.findById(userId)
                .map(RefreshToken::getToken)
                .orElse(null);
    }

    public void delete(String userId) {
        refreshTokenRepository.deleteById(userId);
    }
}
