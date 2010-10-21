package com.glines.socketio.examples.echo;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.glines.socketio.SocketIOServlet;
import com.glines.socketio.SocketIOInbound;

public class EchoSocketServlet extends SocketIOServlet {
	private static final long serialVersionUID = 1L;
	private Set<EchoConnection> connections = new HashSet<EchoConnection>();

	private class EchoConnection implements SocketIOInbound {
		private SocketIOOutbound outbound = null;

		@Override
		public void onConnect(SocketIOOutbound outbound) {
			this.outbound = outbound;
			synchronized (connections) {
				connections.add(this);
			}
		}

		@Override
		public void onDisconnect(boolean timedout) {
			synchronized(this) {
				this.outbound = null;
			}
			synchronized (connections) {
				connections.remove(this);
			}
		}

		@Override
		public void onMessage(String message) {
			try {
				outbound.sendMessage(message);
			} catch (IOException e) {
				outbound.disconnect();
			}
		}
	}

	@Override
	protected SocketIOInbound doSocketIOConnect(HttpServletRequest request,
			String protocol) {
		return new EchoConnection();
	}

}
