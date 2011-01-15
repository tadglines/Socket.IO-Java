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

import org.eclipse.jetty.util.log.Log;

import com.glines.socketio.common.ConnectionState;
import com.glines.socketio.common.DisconnectReason;
import com.glines.socketio.common.SocketIOException;

class SocketIOSessionManager implements SocketIOSession.Factory {
	private static final char[] BASE64_ALPHABET =
	      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
	      .toCharArray();
	private static final int SESSION_ID_LENGTH = 20;

	private static Random random = new SecureRandom();
	private ConcurrentMap<String, SocketIOSession> socketIOSessions = new ConcurrentHashMap<String, SocketIOSession>();
	private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

	private static String generateRandomString(int length) {
	    StringBuilder result = new StringBuilder(length);
	    byte[] bytes = new byte[length];
	    random.nextBytes(bytes);
	    for (int i = 0; i < bytes.length; i++) {
	      result.append(BASE64_ALPHABET[bytes[i] & 0x3F]);
	    }
	    return result.toString();
	}

	private class SessionImpl implements SocketIOSession {
		private final String sessionId;
		private SocketIOInbound inbound;
		private SessionTransportHandler handler = null;
		private ConnectionState state = ConnectionState.CONNECTING;
		private long hbDelay = 0;
		private SessionTask hbDelayTask = null;
		private long timeout = 0;
		private SessionTask timeoutTask = null;
		private boolean timedout = false;
		private AtomicLong messageId = new AtomicLong(0);
		private String closeId = null;

		SessionImpl(String sessionId, SocketIOInbound inbound) {
			this.sessionId = sessionId;
			this.inbound = inbound;
		}

		@Override
		public String generateRandomString(int length) {
			return SocketIOSessionManager.generateRandomString(length);
		}
		
		@Override
		public String getSessionId() {
			return sessionId;
		}

		@Override
		public ConnectionState getConnectionState() {
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
			Log.debug("Session["+sessionId+"]: onTimeout");
			if (!timedout) {
				timedout = true;
				state = ConnectionState.CLOSED;
				onDisconnect(DisconnectReason.TIMEOUT);
				handler.abort();
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
			String data = "" + messageId.incrementAndGet();
			Log.debug("Session["+sessionId+"]: sendPing " + data);
			try {
				handler.sendMessage(new SocketIOFrame(SocketIOFrame.FrameType.PING, 0, data));
			} catch (SocketIOException e) {
				Log.debug("handler.sendMessage failed: ", e);
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
		public long getHeartbeat() {
			return hbDelay;
		}
		
		@Override
		public void setTimeout(long timeout) {
			this.timeout = timeout;
		}

		@Override
		public long getTimeout() {
			return timeout;
		}

		@Override
		public void startClose() {
			state = ConnectionState.CLOSING;
			closeId = "server";
			try {
				handler.sendMessage(new SocketIOFrame(SocketIOFrame.FrameType.CLOSE, 0, closeId));
			} catch (SocketIOException e) {
				Log.debug("handler.sendMessage failed: ", e);
				handler.abort();
			}
		}
		
		@Override
		public void onMessage(SocketIOFrame message) {
			switch (message.getFrameType()) {
			case SESSION_ID:
			case HEARTBEAT_INTERVAL:
				// Ignore these two messages types as they are only intended to be from server to client.
				break;
			case CLOSE:
				Log.debug("Session["+sessionId+"]: onClose: " + message.getData());
				onClose(message.getData());
				break;
			case PING:
				Log.debug("Session["+sessionId+"]: onPing: " + message.getData());
				onPing(message.getData());
				break;
			case PONG:
				Log.debug("Session["+sessionId+"]: onPong: " + message.getData());
				onPong(message.getData());
				break;
			case DATA:
				Log.debug("Session["+sessionId+"]: onMessage: " + message.getData());
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
				handler.sendMessage(new SocketIOFrame(SocketIOFrame.FrameType.PONG, 0, data));
			} catch (SocketIOException e) {
				Log.debug("handler.sendMessage failed: ", e);
				handler.abort();
			}
		}

		@Override
		public void onPong(String data) {
			clearTimeoutTimer();
		}

		@Override
		public void onClose(String data) {
			if (state == ConnectionState.CLOSING) {
				if (closeId != null && closeId.equals(data)) {
					state = ConnectionState.CLOSED;
					onDisconnect(DisconnectReason.CLOSED);
					handler.abort();
				} else {
					try {
						handler.sendMessage(new SocketIOFrame(SocketIOFrame.FrameType.CLOSE, 0, data));
					} catch (SocketIOException e) {
						Log.debug("handler.sendMessage failed: ", e);
						handler.abort();
					}
				}
			} else {
				state = ConnectionState.CLOSING;
				try {
					handler.sendMessage(new SocketIOFrame(SocketIOFrame.FrameType.CLOSE, 0, data));
					handler.disconnectWhenEmpty();
				} catch (SocketIOException e) {
					Log.debug("handler.sendMessage failed: ", e);
					handler.abort();
				}
			}
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
				state = ConnectionState.CLOSED;
				inbound = null;
				socketIOSessions.remove(sessionId);
			} else if (this.handler == null) {
				this.handler = handler;
				try {
					state = ConnectionState.CONNECTED;
					inbound.onConnect(handler);
				} catch (Throwable e) {
					Log.warn("Session["+sessionId+"]: Exception thrown by SocketIOInbound.onConnect()", e);
					state = ConnectionState.CLOSED;
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
					inbound.onMessage(SocketIOFrame.TEXT_MESSAGE_TYPE, message);
				} catch (Throwable e) {
					Log.warn("Session["+sessionId+"]: Exception thrown by SocketIOInbound.onMessage()", e);
				}
			}
		}

		@Override
		public void onDisconnect(DisconnectReason reason) {
			Log.debug("Session["+sessionId+"]: onDisconnect: " + reason);
			clearTimeoutTimer();
			clearHeartbeatTimer();
			if (inbound != null) {
				state = ConnectionState.CLOSED;
				try {
					inbound.onDisconnect(reason, null);
				} catch (Throwable e) {
					Log.warn("Session["+sessionId+"]: Exception thrown by SocketIOInbound.onDisconnect()", e);
				}
				inbound = null;
			}
		}
		
		@Override
		public void onShutdown() {
			Log.debug("Session["+sessionId+"]: onShutdown");
			if (inbound != null) {
				if (state == ConnectionState.CLOSING) {
					if (closeId != null) {
						onDisconnect(DisconnectReason.CLOSE_FAILED);
					} else {
						onDisconnect(DisconnectReason.CLOSED_REMOTELY);
					}
				} else {
					onDisconnect(DisconnectReason.ERROR);
				}
			}
			socketIOSessions.remove(sessionId);
		}
	}
	
	private String generateSessionId() {
		return generateRandomString(SESSION_ID_LENGTH);
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
