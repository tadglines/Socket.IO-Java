package com.glines.socketio.server;

import com.glines.socketio.common.SocketIOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
* @author Mathieu Carbou
*/
public interface TransportHandler extends SocketIOOutbound {
    void init(SocketIOConfig config);
    void setSession(SocketIOSession session);
    
    void handle(HttpServletRequest request, HttpServletResponse response, SocketIOSession session) throws IOException;
    void sendMessage(SocketIOFrame message) throws SocketIOException;
    void disconnectWhenEmpty();
    /**
     * Cause connection and all activity to be aborted and all resources to be released.
     * The handler is expected to call the session's onShutdown() when it is finished.
     * The only session method that the handler can legally call after this is onShutdown();
     */
    void abort();
}
