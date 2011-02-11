package com.glines.socketio.server;

import com.glines.socketio.common.ConnectionState;
import com.glines.socketio.common.DisconnectReason;
import com.glines.socketio.common.SocketIOException;
import com.glines.socketio.util.Web;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
class DefaultSession implements SocketIOSession {

    private static final int SESSION_ID_LENGTH = 20;
    private static final Logger LOGGER = Logger.getLogger(DefaultSession.class.getName());

    private final SocketIOSessionManager socketIOSessionManager;
    private final String sessionId = Web.generateRandomString(SESSION_ID_LENGTH);
    private final AtomicLong messageId = new AtomicLong(0);
    private final Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();

    private SocketIOInbound inbound;
    private TransportHandler handler;
    private ConnectionState state = ConnectionState.CONNECTING;
    private long hbDelay;
    private SessionTask hbDelayTask;
    private long timeout;
    private SessionTask timeoutTask;
    private boolean timedout;
    private String closeId;
    
    DefaultSession(SocketIOSessionManager socketIOSessionManager, SocketIOInbound inbound) {
        this.socketIOSessionManager = socketIOSessionManager;
        this.inbound = inbound;
    }

    @Override
    public void setAttribute(String key, Object val) {
        attributes.put(key, val);
    }

    @Override
    public Object getAttribute(String key) {
        return attributes.get(key);
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
    public TransportHandler getTransportHandler() {
        return handler;
    }

    private void onTimeout() {
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.log(Level.FINE, "Session[" + sessionId + "]: onTimeout");
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
                    DefaultSession.this.onTimeout();
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
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.log(Level.FINE, "Session[" + sessionId + "]: sendPing " + data);
        try {
            handler.sendMessage(new SocketIOFrame(SocketIOFrame.FrameType.PING, 0, data));
        } catch (SocketIOException e) {
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.log(Level.FINE, "handler.sendMessage failed: ", e);
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
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.log(Level.FINE, "handler.sendMessage failed: ", e);
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
                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.log(Level.FINE, "Session[" + sessionId + "]: onClose: " + message.getData());
                onClose(message.getData());
                break;
            case PING:
                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.log(Level.FINE, "Session[" + sessionId + "]: onPing: " + message.getData());
                onPing(message.getData());
                break;
            case PONG:
                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.log(Level.FINE, "Session[" + sessionId + "]: onPong: " + message.getData());
                onPong(message.getData());
                break;
            case DATA:
                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.log(Level.FINE, "Session[" + sessionId + "]: onMessage: " + message.getData());
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
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.log(Level.FINE, "handler.sendMessage failed: ", e);
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
                    if (LOGGER.isLoggable(Level.FINE))
                        LOGGER.log(Level.FINE, "handler.sendMessage failed: ", e);
                    handler.abort();
                }
            }
        } else {
            state = ConnectionState.CLOSING;
            try {
                handler.sendMessage(new SocketIOFrame(SocketIOFrame.FrameType.CLOSE, 0, data));
                handler.disconnectWhenEmpty();
                if ("client".equals(data))
                    onDisconnect(DisconnectReason.CLOSED_REMOTELY);
            } catch (SocketIOException e) {
                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.log(Level.FINE, "handler.sendMessage failed: ", e);
                handler.abort();
            }
        }
    }

    @Override
    public SessionTask scheduleTask(Runnable task, long delay) {
        final Future<?> future = socketIOSessionManager.executor.schedule(task, delay, TimeUnit.MILLISECONDS);
        return new SessionTask() {
            @Override
            public boolean cancel() {
                return future.cancel(false);
            }
        };
    }

    @Override
    public void onConnect(TransportHandler handler) {
        if (handler == null) {
            state = ConnectionState.CLOSED;
            inbound = null;
            socketIOSessionManager.socketIOSessions.remove(sessionId);
        } else if (this.handler == null) {
            this.handler = handler;
            if (inbound == null) {
                state = ConnectionState.CLOSED;
                handler.abort();
            } else {
                try {
                    state = ConnectionState.CONNECTED;
                    inbound.onConnect(handler);
                } catch (Throwable e) {
                    if (LOGGER.isLoggable(Level.WARNING))
                        LOGGER.log(Level.WARNING, "Session[" + sessionId + "]: Exception thrown by SocketIOInbound.onConnect()", e);
                    state = ConnectionState.CLOSED;
                    handler.abort();
                }
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
                if (LOGGER.isLoggable(Level.WARNING))
                    LOGGER.log(Level.WARNING, "Session[" + sessionId + "]: Exception thrown by SocketIOInbound.onMessage()", e);
            }
        }
    }

    @Override
    public void onDisconnect(DisconnectReason reason) {
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.log(Level.FINE, "Session[" + sessionId + "]: onDisconnect: " + reason);
        clearTimeoutTimer();
        clearHeartbeatTimer();
        if (inbound != null) {
            state = ConnectionState.CLOSED;
            try {
                inbound.onDisconnect(reason, null);
            } catch (Throwable e) {
                if (LOGGER.isLoggable(Level.WARNING))
                    LOGGER.log(Level.WARNING, "Session[" + sessionId + "]: Exception thrown by SocketIOInbound.onDisconnect()", e);
            }
            inbound = null;
        }
    }

    @Override
    public void onShutdown() {
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.log(Level.FINE, "Session[" + sessionId + "]: onShutdown");
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
        socketIOSessionManager.socketIOSessions.remove(sessionId);
    }
}
