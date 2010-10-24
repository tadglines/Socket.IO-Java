package com.glines.socketio.client.gwt;

import com.glines.socketio.client.SocketIOConnection;
import com.google.gwt.core.client.JavaScriptObject;

public class GWTSocketIOConnectionImpl implements SocketIOConnection {
	private static final class SocketIOImpl extends JavaScriptObject {
		public static native SocketIOImpl create(GWTSocketIOConnectionImpl impl) /*-{
			var socket = new $wnd.io.Socket(null, {});
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

		@SuppressWarnings("unused")
		protected SocketIOImpl() {
	    }

	    public native void connect() /*-{this.connect();}-*/;

	    public native void disconnect() /*-{this.disconnect();}-*/;

	    public native void send(String data) /*-{this.send(data);}-*/;
	}
	
	
	private final SocketIOConnection.SocketIOConnectionListener listener;
	private SocketIOImpl socket = null;

	GWTSocketIOConnectionImpl(SocketIOConnection.SocketIOConnectionListener listener) {
		this.listener = listener;
	}

	@Override
	public void connect() {
		if (socket != null) {
			throw new IllegalStateException("Already connected");
		}
		socket = SocketIOImpl.create(this);
		socket.connect();
	}
	
	@Override
	public void disconnect() {
		if (socket == null) {
			throw new IllegalStateException("Not connected");
		}
		socket.disconnect();
		socket = null;
	}

	@Override
	public boolean isOpen() {
		return socket != null;
	}

	@Override
	public void sendMessage(String message) {
		if (socket == null) {
			throw new IllegalStateException("Not connected");
		}
		socket.send(message);
	}

	@SuppressWarnings("unused")
	private void onConnect() {
		try {
			listener.onConnect();
		} catch (Throwable t) {
			// Ignore
		}
	}

	@SuppressWarnings("unused")
	private void onDisconnect() {
		socket = null;
		try {
			listener.onDisconnect();
		} catch (Throwable t) {
			// Ignore
		}
	}

	@SuppressWarnings("unused")
	private void onMessage(String message) {
		try {
			listener.onMessage(message);
		} catch (Throwable t) {
			// Ignore
		}
	}
	
}
