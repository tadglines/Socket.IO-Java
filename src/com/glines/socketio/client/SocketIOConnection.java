package com.glines.socketio.client;

public interface SocketIOConnection {
	public enum ConnectionState {
		CONNECTING,
		OPEN,
		CLOSING,
		CLOSED;
	}

	interface SocketIOConnectionListener {
		public abstract void onConnect();
		
		/**
		 * Called when the connection closes or if the initial connection attempt failed.
		 * If the initial connection attempt failed, then wasConnecting will be true.
		 * 
		 * @param wasConnecting
		 */
		public abstract void onDisconnect(boolean wasConnecting);
		public abstract void onMessage(String message);
	}

	/**
	 * Initiate a connection attempt. If the connection succeeds, then the
	 * {@link SocketIOConnectionListener#onConnect() onConnect} will be called. If the connection
	 * attempt fails, then {@link SocketIOConnectionListener#onDisconnect(boolean) onDisonnect} will
	 * be called with a value of true.
	 * @throws IllegalStateException if the socket is not closed.
	 */
	void connect();

	/**
	 * Initiate a disconnect. This does nothing if the socket is already disconnected or in the
	 * process of disconnecting.
	 */
	void disconnect();

	/**
	 * Return the current socket connection state.
	 * @return
	 */
	ConnectionState getConnectionState();

	/**
	 * Send a message.
	 * 
	 * @param message
	 * @throws IllegalStateException if the socket is not connected.
	 */
	void sendMessage(String message);
}
