package com.glines.socketio.client.gwt;

import com.glines.socketio.client.SocketIOConnection;

public class GWTSocketIOConnectionFactory {
	/**
	 * If host is null
	 * @param host
	 * @param port
	 * @return
	 */
	public static SocketIOConnection create(SocketIOConnection.SocketIOConnectionListener listener) {
		return new GWTSocketIOConnectionImpl(listener);
	}
}
