package com.glines.socketio;

import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;

class SocketIOSessionManager implements SocketIOSession.Factory {
	private static final char[] BASE64_ALPHABET =
	      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
	      .toCharArray();
	private static final int SESSION_ID_LENGTH = 20;

	private Random random = new SecureRandom();
	private ScheduledExecutorService executor;
	private ConcurrentMap<String, SocketIOSession> socketIOSessions = new ConcurrentHashMap<String, SocketIOSession>();

	private class SessionImpl implements SocketIOSession {
		private final String sessionId;
		private final SocketIOInbound inbound;
		private SessionTransportHandler handler = null;

		SessionImpl(String sessionId, SocketIOInbound inbound) {
			this.sessionId = sessionId;
			this.inbound = inbound;
		}
		
		@Override
		public String getSessionId() {
			return sessionId;
		}

		@Override
		public SocketIOInbound getInbound() {
			return inbound;
		}

		@Override
		public SessionTransportHandler getTransportHandler() {
			return handler;
		}
		
		@Override
		public void onConnect(SessionTransportHandler handler) {
			if (handler == null) {
				socketIOSessions.remove(sessionId);
			} else {
				this.handler = handler;
				try {
					inbound.onConnect(handler);
				} catch (Throwable e) {
					// Ignore
				}
			}
		}

		@Override
		public void onMessage(String message) {
			try {
				inbound.onMessage(message);
			} catch (Throwable e) {
				// Ignore
			}
		}

		@Override
		public void onDisconnect(boolean timedout) {
			try {
				inbound.onDisconnect(timedout);
			} catch (Throwable e) {
				// Ignore
			}
			socketIOSessions.remove(sessionId);
		}
		
	}
	
	SocketIOSessionManager(int bufferSize, int maxIdleTime) {
		
	}
	
	private String generateSessionId() {
	    StringBuilder result = new StringBuilder(SESSION_ID_LENGTH);
	    byte[] bytes = new byte[SESSION_ID_LENGTH];
	    random.nextBytes(bytes);
	    for (int i = 0; i < bytes.length; i++) {
	      result.append(BASE64_ALPHABET[bytes[i] & 0x3F]);
	    }
	    return result.toString();
	}

	@Override
	public SocketIOSession createSession(SocketIOInbound inbound) {
		SessionImpl impl = new SessionImpl(generateSessionId(), inbound);
		socketIOSessions.put(impl.getSessionId(), impl);
		return impl;
	}

	@Override
	public SocketIOSession getSession(String sessionId) {
		return socketIOSessions.get(sessionId);
	}
}
