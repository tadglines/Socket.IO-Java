package com.glines.socketio;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface SocketIOSession {
	interface Factory {
		SocketIOSession createSession(SocketIOInbound inbound);
		SocketIOSession getSession(String sessionId);
	}

	interface SessionTransportHandler extends SocketIOInbound.SocketIOOutbound {
		void handle(HttpServletRequest request, HttpServletResponse response, SocketIOSession session) throws IOException;
	}
	
	String getSessionId();

	SocketIOInbound getInbound();

	SessionTransportHandler getTransportHandler();
	
	/**
	 * @param handler The handler or null if the connection failed.
	 */
	void onConnect(SessionTransportHandler handler);
	
	void onMessage(String message);
	
	void onDisconnect(boolean timedout);
}
