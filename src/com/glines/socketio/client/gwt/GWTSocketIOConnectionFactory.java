package com.glines.socketio.client.gwt;

import com.glines.socketio.client.common.SocketIOConnection;

public class GWTSocketIOConnectionFactory implements SocketIOConnection.Factory {
	public static SocketIOConnection.Factory INSTANCE = new GWTSocketIOConnectionFactory();

	/**
	 * If host is null
	 * @param host
	 * @param port
	 * @return
	 */
	public SocketIOConnection create(SocketIOConnection.SocketIOConnectionListener listener,
		String host, short port) {
		return new GWTSocketIOConnectionImpl(listener, host, port);
	}
}
