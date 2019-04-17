package org.renci.mobius.controllers;
/*
 * @brief class represents exception raised when a request intended to be processed in future
  *       is identified
  *
 * @author kthare10
 */
public class FutureRequestException extends Exception {
    public FutureRequestException(String message) {
        super(message);
    }
}
