package com.payzapp.walletservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WalletAlreadyExistsException.class)
    public ResponseEntity<?>handleWalletAlreadyExists(WalletAlreadyExistsException ex){

        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }
}
