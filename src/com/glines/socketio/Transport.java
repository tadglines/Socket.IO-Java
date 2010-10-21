package com.glines.socketio;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface Transport {
	/**
	 * @return The name of the transport instance.
	 */
	String getName();

	void init();
	
	void destroy();
	
	void handle(HttpServletRequest request, HttpServletResponse response,
			SocketIOInbound.Factory inboundFactory, SocketIOSession.Factory sessionFactory) throws IOException;
}
