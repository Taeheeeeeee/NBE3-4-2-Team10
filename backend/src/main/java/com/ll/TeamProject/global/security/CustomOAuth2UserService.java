package com.ll.TeamProject.global.security;

import com.ll.TeamProject.domain.user.entity.SiteUser;
import com.ll.TeamProject.domain.user.service.AuthenticationService;
import com.ll.TeamProject.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserService userService;
    private final AuthenticationService authenticationService;

    // 소셜 로그인이 성공할 때마다 이 함수가 실행된다.
    @Transactional
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String oauthId = oAuth2User.getName();

        // 소셜 로그인 종류
        String providerTypeCode = userRequest
                .getClientRegistration()
                .getRegistrationId()
                .toUpperCase(Locale.getDefault());

        Map<String, Object> attributes = oAuth2User.getAttributes();

        String username = providerTypeCode + "__" + oauthId; // KAKAO__12983719287
        String nickname = null;
        String email = null;

        if (providerTypeCode.equals("GOOGLE")) {

            email = (String) attributes.get("email");
            nickname = (String) attributes.get("name");

        } else if (providerTypeCode.equals("KAKAO")) {

            Map<String, String> attributesProperties = (Map<String, String>) attributes.get("properties");
            nickname = attributesProperties.get("nickname");

            Map<String, String> accountProperties = (Map<String, String>) attributes.get("kakao_account");
            email = accountProperties.get("email");
        }

        // 회원이 아니면 회원가입, 회원이면 수정
        SiteUser user = userService.modifyOrJoin(username, nickname, email, providerTypeCode);

        // 최근 로그인 시간
        authenticationService.modifyLastLogin(user);

        return new SecurityUser(
                user.getId(),
                user.getUsername(),
                "",
                user.getNickname(),
                user.getAuthorities()
        );
    }
}

