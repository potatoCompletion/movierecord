package com.my.movierecord.auth.exception;

/**
 * 중복된 아이디로 회원가입을 시도할 때 발생하는 예외.
 * 이미 존재하는 사용자명으로 가입하려고 할 때 throw된다.
 */
public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String username) {
        super("Username already exists: " + username);
    }
}
