package com.glines.socketio.client.gwt;

import com.glines.socketio.client.SocketIOConnection;
import com.google.gwt.core.client.JavaScriptObject;

public class GWTSocketIOConnectionImpl implements SocketIOConnection {
	private static final class SocketIOImpl extends JavaScriptObject {
		public static native SocketIOImpl create(GWTSocketIOConnectionImpl impl, String host, String port) /*-{
			var socket = new $wnd.io.Socket(host, port != null ? {port: port} : {});
			socket.on('connect', $entry(function() {
      			impl.@com.glines.socketio.client.gwt.GWTSocketIOConnectionImpl::onConnect()();
    		}));
			socket.on('message', $entry(function(message) {
      			impl.@com.glines.socketio.client.gwt.GWTSocketIOConnectionImpl::onMessage(Ljava/lang/String;)(message);
    		}));
			socket.on('disconnect', $entry(function() {
      			impl.@com.glines.socketio.client.gwt.GWTSocketIOConnectionImpl::onDisconnect()();
    		}));
    		return socket;
		}-*/;

		protected SocketIOImpl() {
	    }

	    public native void connect() /*-{this.connect();}-*/;

	    public native void disconnect() /*-{this.disconnect();}-*/;

	    public native void _disconnect() /*-{this.transport._disconnect();}-*/;

	    public native void send(String data) /*-{this.send(data);}-*/;

	    public native boolean isConnecting() /*-{return this.connecting;}-*/;

	    public native boolean isConnected() /*-{return this.connected;}-*/;

	    public native boolean isDisconnecting() /*-{return this.disconnecting;}-*/;

	    public native boolean wasConnecting() /*-{return this.wasConnecting;}-*/;

	    public native boolean wasConnected() /*-{return this.wasConnected;}-*/;
	}
	
	
	private final SocketIOConnection.SocketIOConnectionListener listener;
	private final String host;
	private final String port;
	private SocketIOImpl socket = null;
	private ConnectionState state = ConnectionState.CLOSED;

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

		if (ConnectionState.CLOSED != state) {
			throw new IllegalStateException("Invalid connection state X " + state);
		}
		state = ConnectionState.CONNECTING;
		socket.connect();
	}
	
	@Override
	public void disconnect() {
		if (ConnectionState.OPEN == state) {
			state = ConnectionState.CLOSING;
			socket.disconnect();
		} else if (ConnectionState.CONNECTING == state) {
			state = ConnectionState.CLOSED;
			socket._disconnect();
		}
	}

	@Override
	public ConnectionState getConnectionState() {
		return state;
	}

	@Override
	public void sendMessage(String message) {
		if (ConnectionState.OPEN != state) {
			throw new IllegalStateException("Not connected");
		}
		socket.send(message);
	}

	@SuppressWarnings("unused")
	private void onConnect() {
		state = ConnectionState.OPEN;
		listener.onConnect();
	}

	@SuppressWarnings("unused")
	private void onDisconnect() {
		state = ConnectionState.CLOSED;
		listener.onDisconnect(socket.wasConnecting());
	}

	@SuppressWarnings("unused")
	private void onMessage(String message) {
		listener.onMessage(message);
	}
	
}
