/**
 * The MIT License
 * Copyright (c) 2010 Tad Glines
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.glines.socketio.server;

import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.glines.socketio.common.SocketIOMessage;
import com.glines.socketio.server.SocketIOInbound.DisconnectReason;

class SocketIOSessionManager implements SocketIOSession.Factory {
	private static final char[] BASE64_ALPHABET =
	      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
	      .toCharArray();
	private static final int SESSION_ID_LENGTH = 20;

	private Random random = new SecureRandom();
	private ConcurrentMap<String, SocketIOSession> socketIOSessions = new ConcurrentHashMap<String, SocketIOSession>();
	private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

	private class SessionImpl implements SocketIOSession {
		private final String sessionId;
		private SocketIOInbound inbound;
		private SessionTransportHandler handler = null;
		private SessionState state = SessionState.OPENING;
		private long hbDelay = 0;
		private SessionTask hbDelayTask = null;
		private long timeout = 0;
		private SessionTask timeoutTask = null;
		private boolean timedout = false;
		private AtomicLong pingId = new AtomicLong(0);

		SessionImpl(String sessionId, SocketIOInbound inbound) {
			this.sessionId = sessionId;
			this.inbound = inbound;
		}
		
		@Override
		public String getSessionId() {
			return sessionId;
		}

		@Override
		public SessionState getSessionState() {
			return state;
		}

		@Override
		public SocketIOInbound getInbound() {
			return inbound;
		}

		@Override
		public SessionTransportHandler getTransportHandler() {
			return handler;
		}

		private void onTimeout() {
			if (!timedout) {
				timedout = true;
				state = SessionState.CLOSED;
				handler.abort();
				onDisconnect(DisconnectReason.NORMAL);
			}
		}
		
		@Override
		public void startTimeoutTimer() {
			clearTimeoutTimer();
			if (!timedout && timeout > 0) {
				timeoutTask = scheduleTask(new Runnable() {
					@Override
					public void run() {
						SessionImpl.this.onTimeout();
					}
				}, timeout);
			}
		}

		@Override
		public void clearTimeoutTimer() {
			if (timeoutTask != null) {
				timeoutTask.cancel();
				timeoutTask = null;
			}
		}
		
		private void sendPing() {
			String data = "" + pingId.incrementAndGet();
			try {
				handler.sendMessage(SocketIOMessage.encode(SocketIOMessage.Type.PING, data));
			} catch (SocketIOException e) {
				handler.abort();
			}
			startTimeoutTimer();
		}

		@Override
		public void startHeartbeatTimer() {
			clearHeartbeatTimer();
			if (!timedout && hbDelay > 0) {
				hbDelayTask = scheduleTask(new Runnable() {
					@Override
					public void run() {
						sendPing();
					}
				}, hbDelay);
			}
		}

		@Override
		public void clearHeartbeatTimer() {
			if (hbDelayTask != null) {
				hbDelayTask.cancel();
				hbDelayTask = null;
			}
		}

		@Override
		public void setHeartbeat(long delay) {
			hbDelay = delay;
		}

		@Override
		public void setTimeout(long timeout) {
			this.timeout = timeout;
		}

		@Override
		public void onMessage(SocketIOMessage message) {
			switch (message.getType()) {
			case CLOSE:
				onClose(message.getData());
				break;
			case PING:
				onPing(message.getData());
				break;
			case PONG:
				onPong(message.getData());
				break;
			case TEXT:
			case JSON:
				onMessage(message.getData());
				break;
			default:
				// Ignore unknown message types
				break;
			}
		}

		@Override
		public void onPing(String data) {
			try {
				handler.sendMessage(SocketIOMessage.encode(SocketIOMessage.Type.PONG, data));
			} catch (SocketIOException e) {
				handler.abort();
			}
		}

		@Override
		public void onPong(String data) {
			// If data matched that sent in ping, clear heartbeat timer.
			String ping_data = "" + pingId.get();
			if (ping_data.equals(data)) {
				clearTimeoutTimer();
			}
		}

		@Override
		public void onClose(String data) {
			state = SessionState.CLOSED;
			onDisconnect(DisconnectReason.NORMAL);
			handler.abort();
		}

		@Override
		public SessionTask scheduleTask(Runnable task, long delay) {
			final Future<?> future = executor.schedule(task, delay, TimeUnit.MILLISECONDS);
			return new SessionTask() {
				@Override
				public boolean cancel() {
					return future.cancel(false);
				}
			};
		}
		
		@Override
		public void onConnect(SessionTransportHandler handler) {
			if (handler == null) {
				state = SessionState.CLOSED;
				inbound = null;
				socketIOSessions.remove(sessionId);
			} else if (this.handler == null) {
				this.handler = handler;
				try {
					inbound.onConnect(handler);
					state = SessionState.OPEN;
				} catch (Throwable e) {
					state = SessionState.CLOSED;
					handler.abort();
				}
			} else {
				handler.abort();
			}
		}

		@Override
		public void onMessage(String message) {
			if (inbound != null) {
				try {
					inbound.onMessage(message);
				} catch (Throwable e) {
					// Ignore
				}
			}
		}

		@Override
		public void onDisconnect(DisconnectReason reason) {
			clearTimeoutTimer();
			clearHeartbeatTimer();
			if (inbound != null) {
				state = SessionState.CLOSED;
				try {
					inbound.onDisconnect(reason);
				} catch (Throwable e) {
					// Ignore
				}
				inbound = null;
			}
		}
		
		@Override
		public void onShutdown() {
			if (inbound != null) {
				onDisconnect(DisconnectReason.ERROR);
			}
			socketIOSessions.remove(sessionId);
		}
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
