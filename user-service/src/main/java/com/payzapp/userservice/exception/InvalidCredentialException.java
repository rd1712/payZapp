package com.payzapp.userservice.exception;

public class InvalidCredentialException extends RuntimeException {

    public  InvalidCredentialException (String message){
        super(message);
    }
}
