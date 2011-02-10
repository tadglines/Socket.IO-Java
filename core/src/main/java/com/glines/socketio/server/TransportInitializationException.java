package com.glines.socketio.server;

/**
 * @author Mathieu Carbou
 */
public class TransportInitializationException extends RuntimeException {

    private static final long serialVersionUID = -5971560873659475024L;

    public TransportInitializationException(Throwable cause) {
        super(cause);
    }

    public TransportInitializationException(String message) {
        super(message);
    }

    public TransportInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
