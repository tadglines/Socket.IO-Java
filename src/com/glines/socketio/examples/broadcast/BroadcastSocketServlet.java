package com.glines.socketio.examples.broadcast;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.glines.socketio.SocketIOServlet;
import com.glines.socketio.SocketIOInbound;

public class BroadcastSocketServlet extends SocketIOServlet {
	private static final long serialVersionUID = 1L;
	private Set<BroadcastConnection> connections = new HashSet<BroadcastConnection>();

	private class BroadcastConnection implements SocketIOInbound {
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
			broadcast(message);
		}

		private void broadcast(String message) {
			System.out.println("Broadcasting: " + message);
			synchronized (connections) {
				for(BroadcastConnection c: connections) {
					if (c != this) {
						try {
							c.outbound.sendMessage(message);
						} catch (IOException e) {
							c.outbound.disconnect();
						}
					}
				}
			}
		}
	}

	@Override
	protected SocketIOInbound doSocketIOConnect(HttpServletRequest request,
			String protocol) {
		return new BroadcastConnection();
	}

}
