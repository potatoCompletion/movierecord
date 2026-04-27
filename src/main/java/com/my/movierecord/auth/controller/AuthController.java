package com.my.movierecord.auth.controller;

import com.my.movierecord.auth.dto.SignupForm;
import com.my.movierecord.auth.exception.UserAlreadyExistsException;
import com.my.movierecord.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 사용자 인증 관련 HTTP 요청을 처리하는 컨트롤러.
 * 로그인, 회원가입 페이지 렌더링 및 회원가입 처리를 담당한다.
 */
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

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
}
