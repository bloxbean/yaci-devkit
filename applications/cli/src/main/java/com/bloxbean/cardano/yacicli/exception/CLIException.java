package com.bloxbean.cardano.yacicli.exception;

public class CLIException extends Exception {
    public CLIException(String msg, Exception ex) {
        super(msg, ex);
    }
}
