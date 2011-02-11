package com.glines.socketio.server.transport;

import com.glines.socketio.server.SocketIOFrame;
import com.glines.socketio.server.SocketIOSession;
import com.glines.socketio.util.Web;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mathieu Carbou
 */
final class XHRMultipartDataHandler extends AbstractDataHandler {

    private static final Logger LOGGER = Logger.getLogger(XHRMultipartDataHandler.class.getName());
    private static final int MULTIPART_BOUNDARY_LENGTH = 20;
    private static final long DEFAULT_HEARTBEAT_DELAY = 15 * 1000;

    private final SocketIOSession session;

    private long hearbeat;

    private final String contentType;
    private final String boundarySeperator;

    XHRMultipartDataHandler(SocketIOSession session) {
        this.session = session;
        String boundary = Web.generateRandomString(MULTIPART_BOUNDARY_LENGTH);
        boundarySeperator = "--" + boundary;
        contentType = "multipart/x-mixed-replace;boundary=\"" + boundary + "\"";
    }

    @Override
    protected void init() {
        this.hearbeat = getConfig().getHeartbeatDelay(DEFAULT_HEARTBEAT_DELAY);
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine(getConfig().getNamespace() + " data handler configuration:\n" +
                    " - heartbeatDelay=" + hearbeat);
    }

    @Override
    public boolean isConnectionPersistent() {
        return true;
    }

    @Override
    public void onStartSend(HttpServletResponse response) throws IOException {
        response.setContentType(contentType);
        response.setHeader("Connection", "keep-alive");
        ServletOutputStream os = response.getOutputStream();
        os.print(boundarySeperator);
        response.flushBuffer();
    }

    @Override
    public void onWriteData(ServletResponse response, String data) throws IOException {
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.log(Level.FINE, "Session[" + session.getSessionId() + "]: writeData(START): " + data);
        ServletOutputStream os = response.getOutputStream();
        os.println("Content-Type: text/plain");
        os.println();
        os.println(data);
        os.println(boundarySeperator);
        response.flushBuffer();
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.log(Level.FINE, "Session[" + session.getSessionId() + "]: writeData(END): " + data);
    }

    @Override
    public void onFinishSend(ServletResponse response) throws IOException {
    }

    @Override
    public void onConnect(HttpServletRequest request, HttpServletResponse response) throws IOException {
        onStartSend(response);
        onWriteData(response, SocketIOFrame.encode(SocketIOFrame.FrameType.SESSION_ID, 0, session.getSessionId()));
        onWriteData(response, SocketIOFrame.encode(SocketIOFrame.FrameType.HEARTBEAT_INTERVAL, 0, "" + hearbeat));
    }
}
