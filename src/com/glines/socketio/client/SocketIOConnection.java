package com.glines.socketio.client;

public interface SocketIOConnection {
	interface SocketIOConnectionListener {
		public abstract void onConnect();
		public abstract void onDisconnect();
		public abstract void onMessage(String message);
	}

	void connect();
	void disconnect();
	boolean isOpen();
	void sendMessage(String message);
}
