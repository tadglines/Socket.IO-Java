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
