package com.qzero.mcga.exception;

/**
 * Exception that is caused by known reason and can be directly shown to client side
 */
public class ResponsiveException extends Exception {

    private int errorCode;
    private String errorMessage;
    private Throwable additionalException;

    public ResponsiveException(int errorCode, String errorMessage) {
        super("Code:"+errorCode+"\nReason:"+errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public ResponsiveException(int errorCode, String errorMessage, Throwable additionalException) {
        super("Code:"+errorCode+"\nReason:"+errorMessage+"\nExceptionMessage:"+additionalException.getMessage(), additionalException);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.additionalException = additionalException;
    }

    public ResponsiveException(String errorMessage) {
        this(-1, errorMessage);
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
