package org.renci.controllers.os;

public class OpenstackException extends Exception{

    private Integer status;
    public Integer getStatus() { return status; }
    public OpenstackException() {}

    public OpenstackException(Throwable throwable) {
        super(throwable);
    }

    public OpenstackException(Integer s, String message) {
        super(message);
        status = s;
    }
    public OpenstackException(String message) {
        super(message);
        status =  500;
    }
}