package ru.fix.multiplexer.exception;

public class BufferException extends RuntimeException {

    public BufferException(String message) {
        super(message);
    }

    public BufferException(String message, Throwable cause) {
        super(message, cause);
    }

    public BufferException(Throwable cause) {
        super(cause);
    }

    public BufferException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
