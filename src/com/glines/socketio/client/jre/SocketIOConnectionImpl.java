package com.glines.socketio.client.jre;

import org.eclipse.jetty.client.HttpClient;

import com.glines.socketio.client.common.SocketIOConnection;
import com.glines.socketio.common.CloseType;
import com.glines.socketio.common.ConnectionState;
import com.glines.socketio.common.DisconnectReason;
import com.glines.socketio.common.SocketIOException;
import com.glines.socketio.common.SocketIOMessageParser;

public class SocketIOConnectionImpl implements SocketIOConnection {
	private final SocketIOConnection.SocketIOConnectionListener listener;
	private final String host;
	private final short port;
	private HttpClient client;

	public SocketIOConnectionImpl(SocketIOConnection.SocketIOConnectionListener listener, 
			String host, short port) {
		this.listener = listener;
		this.host = host;
		this.port = port;
	}
	
	@Override
	public void connect() {
		if (client == null) {
			client = new HttpClient();
			client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
			try {
				client.start();
			} catch (Exception e) {
				client = null;
				listener.onDisconnect(DisconnectReason.ERROR, "Failed to initialize");
			}
		}
		
	}

	@Override
	public void close(CloseType closeType) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void disconnect() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ConnectionState getConnectionState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void sendMessage(String message) throws SocketIOException {
		sendMessage(0, message);
	}

	@Override
	public void sendMessage(int messageType, Object message) throws SocketIOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setMessageParser(int messageType, SocketIOMessageParser parser) {
		// TODO Auto-generated method stub
		
	}
}
