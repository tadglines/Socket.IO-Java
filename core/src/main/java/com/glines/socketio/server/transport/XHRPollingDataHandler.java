package com.glines.socketio.server.transport;

import com.glines.socketio.server.SocketIOFrame;
import com.glines.socketio.server.SocketIOSession;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mathieu Carbou
 */
final class XHRPollingDataHandler extends AbstractDataHandler {

    /**
     * For non persistent connection transports, this is the amount of time to wait
     * for messages before returning empty results.
     */
    private static final long DEFAULT_TIMEOUT = 20 * 1000;
    private static final Logger LOGGER = Logger.getLogger(XHRPollingDataHandler.class.getName());

    private final SocketIOSession session;
    private long timeout;

    XHRPollingDataHandler(SocketIOSession session) {
        this.session = session;
    }

    @Override
    protected void init() {
        this.timeout = getConfig().getTimeout(DEFAULT_TIMEOUT);
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine(getConfig().getNamespace() + " data handler configuration:\n" +
                    " - timeout=" + timeout);
    }

    @Override
    public boolean isConnectionPersistent() {
        return false;
    }

    @Override
    public void onStartSend(HttpServletResponse response) throws IOException {
        response.setContentType("text/plain; charset=UTF-8");
    }

    @Override
    public void onWriteData(ServletResponse response, String data) throws IOException {
        response.getOutputStream().print(data);
        response.flushBuffer();
    }

    @Override
    public void onFinishSend(ServletResponse response) throws IOException {
    }

    @Override
    public void onConnect(HttpServletRequest request, HttpServletResponse response) throws IOException {
        onStartSend(response);
        onWriteData(response, SocketIOFrame.encode(SocketIOFrame.FrameType.SESSION_ID, 0, session.getSessionId()));
        onWriteData(response, SocketIOFrame.encode(SocketIOFrame.FrameType.HEARTBEAT_INTERVAL, 0, "" + timeout));
    }
}
