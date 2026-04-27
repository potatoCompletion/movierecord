package com.my.movierecord.auth.service;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String username) {
        super("Username already exists: " + username);
    }
}
