package org.renci.mobius.controllers;

import org.springframework.http.HttpStatus;

public class MobiusException extends Exception{

    private HttpStatus status;
    public HttpStatus getStatus() { return status; }
    public MobiusException() {}

    public MobiusException(Throwable throwable) {
        super(throwable);
    }

    public MobiusException(HttpStatus s, String message) {
        super(message);
        status = s;
    }
    public MobiusException(String message) {
        super(message);
        status =  HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
