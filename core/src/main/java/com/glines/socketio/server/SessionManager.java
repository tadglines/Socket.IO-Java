package com.glines.socketio.server;

/**
* @author Mathieu Carbou (mathieu.carbou@gmail.com)
*/
public interface SessionManager {
    SocketIOSession createSession(SocketIOInbound inbound);
    SocketIOSession getSession(String sessionId);
}
