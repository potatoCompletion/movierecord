package com.my.movierecord.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 회원가입 폼 데이터를 담는 DTO.
 * 사용자명, 비밀번호, 비밀번호 확인 정보의 유효성 검증을 포함한다.
 */
@Getter
@Setter
public class SignupForm {

    // 사용자명: 3~20자
    @NotBlank(message = "아이디를 입력해주세요.")
    @Size(min = 3, max = 20, message = "아이디는 3~20자 사이여야 합니다.")
    private String username;

    // 비밀번호: 8~72자 (BCrypt의 최대 길이)
    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Size(min = 8, max = 72, message = "비밀번호는 8~72자 사이여야 합니다.")
    private String password;

    // 비밀번호 확인 (컨트롤러에서 일치 여부 검증)
    @NotBlank(message = "비밀번호 확인을 입력해주세요.")
    private String passwordConfirm;
}
