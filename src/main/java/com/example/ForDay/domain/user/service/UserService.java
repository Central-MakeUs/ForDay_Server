package com.example.ForDay.domain.user.service;

import com.example.ForDay.domain.auth.dto.KakaoProfileDto;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.domain.user.type.Role;
import com.example.ForDay.domain.user.type.SocialType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User getUserBySocialId(String id) {
        return userRepository.findBySocialId(id);
    }

    @Transactional
    public User createOauth(String id, KakaoProfileDto.KakaoAccount kakaoAccount, SocialType socialType) {
        return userRepository.save(User.builder()
                .role(Role.USER)
                .email(kakaoAccount.getEmail())
                .socialType(socialType)
                .socialId(id)
                .build());
    }

}
