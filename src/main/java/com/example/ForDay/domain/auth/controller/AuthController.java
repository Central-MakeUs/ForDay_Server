package com.example.ForDay.domain.auth.controller;

import com.example.ForDay.domain.auth.dto.AccessTokenDto;
import com.example.ForDay.domain.auth.dto.KakaoProfileDto;
import com.example.ForDay.domain.auth.dto.request.KakaoLoginReqDto;
import com.example.ForDay.domain.auth.dto.request.RefreshRequestDto;
import com.example.ForDay.domain.auth.dto.response.LoginResDto;
import com.example.ForDay.domain.auth.service.KakaoService;
import com.example.ForDay.domain.auth.service.RefreshTokenService;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.service.UserService;
import com.example.ForDay.domain.user.type.Role;
import com.example.ForDay.domain.user.type.SocialType;
import com.example.ForDay.global.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Auth", description = "인증 / 로그인 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final KakaoService kakaoService;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Operation(
            summary = "카카오 로그인"
    )
    @ApiResponse(
            responseCode = "200",
            description = "로그인 성공",
            content = @Content(schema = @Schema(implementation = LoginResDto.class))
    )
    @PostMapping("/kakao")
    public LoginResDto kakaoLogin(@RequestBody @Valid KakaoLoginReqDto reqDto) {
        // (사용자 정보 요청을 위한) 카카오 accessToken 발급
        AccessTokenDto accessTokenDto = kakaoService.getAccessToken(reqDto.getCode());

        // accessToken을 활용하여 카카오 사용자 정보 얻기
        KakaoProfileDto kakaoProfileDto = kakaoService.getKakaoProfile(accessTokenDto.getAccess_token());

        boolean isNewUser = false;
        // 회원가입이 되어 있지 않다면 회원가입
        User originalUser = userService.getUserBySocialId(kakaoProfileDto.getId());
        if(originalUser == null) {
            isNewUser = true;
            // 회원가입
            originalUser = userService.createOauth(kakaoProfileDto.getId(), kakaoProfileDto.getKakao_account(), SocialType.KAKAO);
        }

        // 회원 가입 되어 있는 경우 -> 토큰 발급
        String accessToken = jwtUtil.createAccessToken(originalUser.getSocialId(), Role.USER);
        String refreshToken = jwtUtil.createRefreshToken(originalUser.getSocialId());

        refreshTokenService.save(originalUser.getId(), refreshToken);

        return new LoginResDto(accessToken, refreshToken, isNewUser);

    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refresh(@RequestBody @Valid RefreshRequestDto reqDto) {
        return ResponseEntity.ok("ok");
    }

}
