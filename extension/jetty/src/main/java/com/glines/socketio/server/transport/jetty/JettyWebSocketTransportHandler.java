/**
 * The MIT License
 * Copyright (c) 2010 Tad Glines
 *
 * Contributors: Ovea.com, Mycila.com
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
package com.glines.socketio.server.transport.jetty;

import com.glines.socketio.annotation.Handle;
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
@Handle({TransportType.WEB_SOCKET, TransportType.FLASH_SOCKET})
public final class JettyWebSocketTransportHandler extends AbstractTransportHandler implements WebSocket.OnTextMessage, WebSocket.OnBinaryMessage {

    private static final long DEFAULT_HEARTBEAT_DELAY = SocketIOConfig.DEFAULT_MAX_IDLE / 2;
    private static final long DEFAULT_HEARTBEAT_TIMEOUT = 10 * 1000;

    private static final Logger LOGGER = Logger.getLogger(JettyWebSocketTransportHandler.class.getName());

    private Connection outbound;
    private boolean initiated;

    @Override
    protected void init() {
        getSession().setHeartbeat(getConfig().getHeartbeatDelay(DEFAULT_HEARTBEAT_DELAY));
        getSession().setTimeout(getConfig().getHeartbeatTimeout(DEFAULT_HEARTBEAT_TIMEOUT));
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine(getConfig().getNamespace() + " transport handler configuration:\n" +
                    " - heartbeatDelay=" + getSession().getHeartbeat() + "\n" +
                    " - heartbeatTimeout=" + getSession().getTimeout());
    }

    @Override
    public void onOpen(Connection connection) {
      this.outbound = connection;
    }

    @Override
    public void onClose(int closeCode, String message) {
        getSession().onShutdown();
    }

    @Override
    public void onMessage(String message) {
        getSession().startHeartbeatTimer();
        if (!initiated) {
            if ("OPEN".equals(message)) {
                try {
                    outbound.sendMessage(SocketIOFrame.encode(SocketIOFrame.FrameType.SESSION_ID, 0, getSession().getSessionId()));
                    outbound.sendMessage(SocketIOFrame.encode(SocketIOFrame.FrameType.HEARTBEAT_INTERVAL, 0, "" + getSession().getHeartbeat()));
                    getSession().onConnect(this);
                    initiated = true;
                } catch (IOException e) {
                    outbound.disconnect();
                    getSession().onShutdown();
                }
            } else {
                outbound.disconnect();
                getSession().onShutdown();
            }
        } else {
            List<SocketIOFrame> messages = SocketIOFrame.parse(message);
            for (SocketIOFrame msg : messages) {
                getSession().onMessage(msg);
            }
        }
    }

    @Override
    public void onMessage(byte[] data, int offset, int length) {
        try {
            onMessage(new String(data, offset, length, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // Do nothing for now.
        }
    }

    @Override
    public void disconnect() {
        getSession().onDisconnect(DisconnectReason.DISCONNECT);
        outbound.disconnect();
    }

    @Override
    public void close() {
        getSession().startClose();
    }

    @Override
    public ConnectionState getConnectionState() {
        return getSession().getConnectionState();
    }

    @Override
    public void sendMessage(SocketIOFrame frame) throws SocketIOException {
        if (outbound.isOpen()) {
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.log(Level.FINE, "Session[" + getSession().getSessionId() + "]: sendMessage: [" + frame.getFrameType() + "]: " + frame.getData());
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
        if (outbound.isOpen() && getSession().getConnectionState() == ConnectionState.CONNECTED) {
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
        getSession().onShutdown();
    }
}
