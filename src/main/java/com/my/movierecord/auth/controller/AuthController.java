package com.my.movierecord.auth.controller;

import com.my.movierecord.auth.dto.SignupForm;
import com.my.movierecord.auth.dto.TokenPair;
import com.my.movierecord.auth.exception.InvalidRefreshTokenException;
import com.my.movierecord.auth.exception.UserAlreadyExistsException;
import com.my.movierecord.auth.security.CookieUtil;
import com.my.movierecord.auth.service.TokenService;
import com.my.movierecord.auth.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 사용자 인증 관련 HTTP 요청을 처리하는 컨트롤러.
 * 로그인, 회원가입 페이지 렌더링 및 회원가입 처리를 담당한다.
 */
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final TokenService tokenService;
    private final CookieUtil cookieUtil;

    /**
     * 로그인 폼 페이지를 렌더링한다.
     */
    @GetMapping("/login")
    public String loginForm() {
        return "auth/login";
    }

    /**
     * 회원가입 폼 페이지를 렌더링한다.
     */
    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("signupForm", new SignupForm());
        return "auth/signup";
    }

    /**
     * 사용자 회원가입 요청을 처리한다.
     * 비밀번호 확인 검증 후 서비스를 통해 사용자를 저장한다.
     * 중복된 아이디인 경우 예외를 처리하여 폼에 오류 메시지를 표시한다.
     */
    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute SignupForm signupForm, BindingResult result) {
        // 비밀번호와 비밀번호 확인이 일치하는지 검증
        if (!result.hasFieldErrors("password") && !result.hasFieldErrors("passwordConfirm")
                && !signupForm.getPassword().equals(signupForm.getPasswordConfirm())) {
            result.rejectValue("passwordConfirm", "mismatch", "비밀번호가 일치하지 않습니다.");
        }
        if (result.hasErrors()) {
            return "auth/signup";
        }
        try {
            userService.signup(signupForm);
        } catch (UserAlreadyExistsException e) {
            result.rejectValue("username", "duplicate", "이미 사용 중인 아이디입니다.");
            return "auth/signup";
        }
        return "redirect:/auth/login?signupSuccess";
    }

    /**
     * 리프레시 토큰 회전. 유효한 {@code REFRESH_TOKEN} 쿠키를 받아 새 액세스/리프레시 쿠키를 발급한다.
     * 없거나 폐기·만료된 토큰이면 {@link InvalidRefreshTokenException} 등이 발생해 401 로 처리된다.
     */
    @PostMapping("/token/refresh")
    @ResponseBody
    public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
        String rawRefreshToken = cookieUtil.readRefreshToken(request)
                .orElseThrow(() -> new InvalidRefreshTokenException("Missing refresh token cookie"));
        TokenPair tokens = tokenService.refresh(rawRefreshToken);
        cookieUtil.writeTokens(response, tokens);
        return ResponseEntity.noContent().build();
    }

    /**
     * 로그아웃. 리프레시 토큰을 폐기(DB revoke + Redis 삭제)하고 인증 쿠키를 즉시 만료시킨다.
     */
    @PostMapping("/logout")
    @ResponseBody
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        cookieUtil.readRefreshToken(request).ifPresent(tokenService::revoke);
        cookieUtil.clearTokens(response);
        return ResponseEntity.noContent().build();
    }
}
