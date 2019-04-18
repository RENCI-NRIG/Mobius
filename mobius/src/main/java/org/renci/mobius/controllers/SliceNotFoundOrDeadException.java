package org.renci.mobius.controllers;

/*
 * @brief class represents exception raised when slice is not found or dead
 *
 * @author kthare10
 */
public class SliceNotFoundOrDeadException extends Exception {
    public SliceNotFoundOrDeadException(String message) {
        super(message);
    }
}
