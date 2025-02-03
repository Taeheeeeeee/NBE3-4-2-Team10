package com.ll.TeamProject.global.rq;

import com.ll.TeamProject.domain.user.entity.SiteUser;
import com.ll.TeamProject.domain.user.service.UserService;
import com.ll.TeamProject.global.security.SecurityUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Arrays;
import java.util.Optional;

@RequestScope
@Component
@RequiredArgsConstructor
public class Rq {

    private final HttpServletResponse resp;
    private final HttpServletRequest req;
    private final UserService userService;

    // 쿠기 생성
    public void setCookie(String name, String value) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .path("/")
                .domain("localhost")
                .sameSite("None") // 소셜 로그인 중 오류 생겨서 변경
                .httpOnly(true)
                // secure로 설정한 쿠키는 https 에서만 보내지는 것 같아서 프론트 문제 생김, 임시 주석처리
                .secure(true)
                .build();

        resp.addHeader("Set-Cookie", cookie.toString());
    }

    // 요청에서 헤더 얻어오기
    public String getHeader(String name) {
        return req.getHeader(name);
    }

    // 쿠키 값 얻기
    public String getCookieValue(String name) {
        return Optional
                .ofNullable(req.getCookies())
                .stream()
                .flatMap(cookies -> Arrays.stream(cookies))
                .filter(cookie -> cookie.getName().equals(name))
                .map(cookie -> cookie.getValue())
                .findFirst()
                .orElse(null);
    }

    // 응답 헤더 설정
    public void setHeader(String name, String value) {
        resp.setHeader(name, value);
    }

    // 로그인 설정
    public void setLogin(SiteUser user) {
        // 스프링 시큐리티가 이해하는 user로 변경
        UserDetails userDetails = new SecurityUser(
                user.getId(),
                user.getUsername(),
                "",
                user.getNickname(),
                user.getAuthorities()
        );

        // 인증
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                userDetails.getPassword(),
                userDetails.getAuthorities()
        );

        // 인증 저장
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    // 쿠키 삭제
    public void deleteCookie(String name) {
        ResponseCookie cookie = ResponseCookie.from(name, null)
                .path("/")
                .domain("localhost")
                .sameSite("None") // 소셜 로그인 작업 중 변경
                .secure(true)
                .httpOnly(true)
                .maxAge(0)
                .build();

        resp.addHeader("Set-Cookie", cookie.toString());
    }

    // 요청을 보낸 사용자의 인증 정보를 가져와 해당 사용자를 조회, 시큐리티 내부에 인증된 사용자를 반환
    public SiteUser getActor() {
        return Optional.ofNullable(
                        SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                )
                .map(Authentication::getPrincipal)
                .filter(principal -> principal instanceof SecurityUser)
                .map(principal -> (SecurityUser) principal)
                .map(securityUser -> new SiteUser(securityUser.getId(), securityUser.getUsername()))
                .orElse(null);
    }

    // JWT 생성하고 쿠키 생성
    public String makeAuthCookies(SiteUser user) {
        String accessToken = userService.genAccessToken(user);

        System.out.println("쿠키를 설정합니다.");
        setCookie("apiKey", user.getApiKey());
        setCookie("accessToken", accessToken);

        return accessToken;
    }

    // 요청을 보낸 사용자의 인증 정보를 가져와 실제 DB에 저장된 user 찾기
    public Optional<SiteUser> findByActor() {
        SiteUser actor = getActor();

        if (actor == null) {
            return Optional.empty();
        }

        return userService.findById(actor.getId());
    }
}
