package com.glines.socketio.server;

/**
 * @author Mathieu Carbou
 */
public class SessionTransportInitializationException extends RuntimeException {

    private static final long serialVersionUID = -5971560873659475024L;

    public SessionTransportInitializationException(Throwable cause) {
        super(cause);
    }

    public SessionTransportInitializationException(String message) {
        super(message);
    }

    public SessionTransportInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
