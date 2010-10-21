package com.glines.socketio.examples.chat;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.util.ajax.JSON;

import com.glines.socketio.SocketIOException;
import com.glines.socketio.SocketIOServlet;
import com.glines.socketio.SocketIOInbound;

public class ChatSocketServlet extends SocketIOServlet {
	private static final long serialVersionUID = 1L;
	private AtomicInteger ids = new AtomicInteger(1);
	private Set<ChatConnection> connections = new HashSet<ChatConnection>();

	private class ChatConnection implements SocketIOInbound {
		private SocketIOOutbound outbound = null;
		private Integer sessionId = ids.getAndIncrement();

		@Override
		public void onConnect(SocketIOOutbound outbound) {
			this.outbound = outbound;
			synchronized (connections) {
				connections.add(this);
			}
			try {
				outbound.sendMessage("~j~" + JSON.toString(
						Collections.singletonMap("buffer", new String[]{})));
			} catch (SocketIOException e) {
				onDisconnect(false);
			}
			broadcast("~j~" + JSON.toString(
					Collections.singletonMap("announcement", sessionId + " connected")));
		}

		@Override
		public void onDisconnect(boolean timedout) {
			synchronized(this) {
				this.outbound = null;
			}
			synchronized (connections) {
				connections.remove(this);
			}
			broadcast("~j~" + JSON.toString(
					Collections.singletonMap("announcement", sessionId + " disconnected")));
		}

		@Override
		public void onMessage(String message) {
			System.out.println("Recieved: " + message);
			broadcast("~j~" + JSON.toString(
					Collections.singletonMap("message",
							new String[]{sessionId.toString(), message})));
		}

		private void broadcast(String message) {
			System.out.println("Broadcasting: " + message);
			synchronized (connections) {
				for(ChatConnection c: connections) {
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
		return new ChatConnection();
	}

}
