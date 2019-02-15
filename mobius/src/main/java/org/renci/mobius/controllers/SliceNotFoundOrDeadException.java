package org.renci.mobius.controllers;

public class SliceNotFoundOrDeadException extends Exception {
    public SliceNotFoundOrDeadException(String message) {
        super(message);
    }
}
