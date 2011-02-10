package com.glines.socketio.container.jetty;

import com.glines.socketio.common.ConnectionState;
import com.glines.socketio.common.DisconnectReason;
import com.glines.socketio.common.SocketIOException;
import com.glines.socketio.server.*;
import org.eclipse.jetty.websocket.WebSocket;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mathieu Carbou
 */
final class JettyWebSocketSessionAdapter extends AbstractSessionTransportHandler implements WebSocket {

    private static final Logger LOGGER = Logger.getLogger(JettyWebSocketSessionAdapter.class.getName());

    private final SocketIOSession session;

    private Outbound outbound;
    private boolean initiated;

    JettyWebSocketSessionAdapter(SocketIOSession session) {
        this.session = session;
    }

    @Override
    protected void init() throws SessionTransportInitializationException {
        session.setHeartbeat(getConfig().getHeartbeat());
        session.setTimeout(getConfig().getTimeout());
    }

    @Override
    public void onConnect(final Outbound outbound) {
        this.outbound = outbound;
    }

    @Override
    public void onDisconnect() {
        session.onShutdown();
    }

    @Override
    public void onMessage(byte frame, String message) {
        session.startHeartbeatTimer();
        if (!initiated) {
            if ("OPEN".equals(message)) {
                try {
                    outbound.sendMessage(SocketIOFrame.encode(SocketIOFrame.FrameType.SESSION_ID, 0, session.getSessionId()));
                    outbound.sendMessage(SocketIOFrame.encode(SocketIOFrame.FrameType.HEARTBEAT_INTERVAL, 0, "" + session.getHeartbeat()));
                    session.onConnect(this);
                    initiated = true;
                } catch (IOException e) {
                    outbound.disconnect();
                    session.onShutdown();
                }
            } else {
                outbound.disconnect();
                session.onShutdown();
            }
        } else {
            List<SocketIOFrame> messages = SocketIOFrame.parse(message);
            for (SocketIOFrame msg : messages) {
                session.onMessage(msg);
            }
        }
    }

    @Override
    public void onMessage(byte frame, byte[] data, int offset, int length) {
        try {
            onMessage(frame, new String(data, offset, length, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // Do nothing for now.
        }
    }

    @Override
    public void onFragment(boolean more, byte opcode, byte[] data, int offset, int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void disconnect() {
        session.onDisconnect(DisconnectReason.DISCONNECT);
        outbound.disconnect();
    }

    @Override
    public void close() {
        session.startClose();
    }

    @Override
    public ConnectionState getConnectionState() {
        return session.getConnectionState();
    }

    @Override
    public void sendMessage(SocketIOFrame frame) throws SocketIOException {
        if (outbound.isOpen()) {
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.log(Level.FINE, "Session[" + session.getSessionId() + "]: sendMessage: [" + frame.getFrameType() + "]: " + frame.getData());
            try {
                outbound.sendMessage(frame.encode());
            } catch (IOException e) {
                outbound.disconnect();
                throw new SocketIOException(e);
            }
        } else {
            throw new SocketIOClosedException();
        }
    }

    @Override
    public void sendMessage(String message) throws SocketIOException {
        sendMessage(SocketIOFrame.TEXT_MESSAGE_TYPE, message);
    }

    @Override
    public void sendMessage(int messageType, String message)
            throws SocketIOException {
        if (outbound.isOpen() && session.getConnectionState() == ConnectionState.CONNECTED) {
            sendMessage(new SocketIOFrame(SocketIOFrame.FrameType.DATA, messageType, message));
        } else {
            throw new SocketIOClosedException();
        }
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, SocketIOSession session) throws IOException {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unexpected request on upgraded WebSocket connection");
    }

    @Override
    public void abort() {
        outbound.disconnect();
        outbound = null;
        session.onShutdown();
    }
}
