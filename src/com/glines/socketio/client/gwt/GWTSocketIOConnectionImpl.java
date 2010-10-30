package com.glines.socketio.client.gwt;

import com.glines.socketio.client.common.SocketIOConnection;
import com.glines.socketio.common.ConnectionState;
import com.glines.socketio.common.DisconnectReason;
import com.glines.socketio.common.SocketIOException;
import com.google.gwt.core.client.JavaScriptObject;

public class GWTSocketIOConnectionImpl implements SocketIOConnection {
	private static final class SocketIOImpl extends JavaScriptObject {
		public static native SocketIOImpl create(GWTSocketIOConnectionImpl impl, String host, String port) /*-{
			var socket = new $wnd.io.Socket(host, port != null ? {port: port} : {});
			socket.on('connect', $entry(function() {
      			impl.@com.glines.socketio.client.gwt.GWTSocketIOConnectionImpl::onConnect()();
    		}));
			socket.on('message', $entry(function(messageType, message) {
      			impl.@com.glines.socketio.client.gwt.GWTSocketIOConnectionImpl::onMessage(ILjava/lang/String;)(messageType, message);
    		}));
			socket.on('disconnect', $entry(function(dr, message) {
      			impl.@com.glines.socketio.client.gwt.GWTSocketIOConnectionImpl::onDisconnect(ILjava/lang/String;)(dr, message);
    		}));
    		return socket;
		}-*/;

		protected SocketIOImpl() {
	    }

	    public native int getSocketState() /*-{this.socketState;}-*/;

	    public native void connect() /*-{this.connect();}-*/;

	    public native void close() /*-{this.close();}-*/;

	    public native void disconnect() /*-{this.disconnect();}-*/;

	    public native void send(int messageType, String data) /*-{this.send(messageType, data);}-*/;
	}
	
	
	private final SocketIOConnection.SocketIOConnectionListener listener;
	private final String host;
	private final String port;
	private SocketIOImpl socket = null;

	GWTSocketIOConnectionImpl(SocketIOConnection.SocketIOConnectionListener listener,
			String host, short port) {
		this.listener = listener;
		this.host = host;
		if (port > 0) {
			this.port = "" + port;
		} else {
			this.port = null;
		}
	}

	@Override
	public void connect() {
		if (socket == null) {
			socket = SocketIOImpl.create(this, host, port);
		}

		if (ConnectionState.CLOSED != getConnectionState()) {
			throw new IllegalStateException("Connection isn't closed!");
		}
		socket.connect();
	}

	@Override
	public void close() {
		if (ConnectionState.CONNECTED == getConnectionState()) {
			socket.close();
		}
	}
	
	@Override
	public void disconnect() {
		if (ConnectionState.CLOSED != getConnectionState()) {
			socket.disconnect();
		}
	}

	@Override
	public ConnectionState getConnectionState() {
		ConnectionState state;
		if (socket != null) {
			state = ConnectionState.fromInt(socket.getSocketState());
		} else {
			state = ConnectionState.CLOSED;
		}
		return state;
	}

	@Override
	public void sendMessage(String message) throws SocketIOException {
		sendMessage(0, message);
	}

	@Override
	public void sendMessage(int messageType, String message)
			throws SocketIOException {
		if (ConnectionState.CONNECTED != getConnectionState()) {
			throw new IllegalStateException("Not connected");
		}
		socket.send(messageType, message);
	}

	@SuppressWarnings("unused")
	private void onConnect() {
		try {
			listener.onConnect();
		} catch (Exception e) {
			// Ignore
		}
	}

	@SuppressWarnings("unused")
	private void onDisconnect(int dr, String errorMessage) {
		DisconnectReason reason = DisconnectReason.fromInt(dr);
		try {
			listener.onDisconnect(reason, errorMessage);
		} catch (Exception e) {
			// Ignore
		}
	}

	@SuppressWarnings("unused")
	private void onMessage(int messageType, String message) {
		try {
			listener.onMessage(messageType, message);
		} catch (Exception e) {
			// Ignore
		}
	}
	
}
