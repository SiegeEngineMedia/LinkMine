package com.sem.linkmine.errors;

public class AuthenticationException extends Exception {
    public AuthenticationException() {
        super("Not allowed");
    }
}
