package com.payzapp.walletservice.exception;

public class WalletDoesntExistException extends  RuntimeException{
    public WalletDoesntExistException(String message){
        super(message);
    }
}
