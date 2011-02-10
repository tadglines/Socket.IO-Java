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
package com.glines.socketio.container.jetty;

import com.glines.socketio.server.*;
import com.glines.socketio.util.Web;
import org.eclipse.jetty.websocket.WebSocketFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class JettyWebSocketTransport extends AbstractTransport {

    private final WebSocketFactory wsFactory = new WebSocketFactory();

    @Override
    public void init() throws TransportInitializationException {
        setBufferSize(getConfig().getBufferSize());
        setMaxIdleTime(getConfig().getMaxIdle());
    }

    @Override
    public String getName() {
        return SocketIOConfig.KEY_TRANSPORT_WEBSOCKET;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       Transport.InboundFactory inboundFactory,
                       SocketIOSession.Factory sessionFactory) throws IOException {

        String sessionId = Web.extractSessionId(request);

        if ("GET".equals(request.getMethod()) && sessionId == null && "WebSocket".equals(request.getHeader("Upgrade"))) {
            boolean hixie = request.getHeader("Sec-WebSocket-Key1") != null;

            String protocol = request.getHeader(hixie ? "Sec-WebSocket-Protocol" : "WebSocket-Protocol");
            if (protocol == null)
                protocol = request.getHeader("Sec-WebSocket-Protocol");

            String host = request.getHeader("Host");
            String origin = request.getHeader("Origin");
            if (origin == null) {
                origin = host;
            }

            SocketIOInbound inbound = inboundFactory.getInbound(request);
            if (inbound == null) {
                if (hixie) {
                    response.setHeader("Connection", "close");
                }
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            } else {
                SocketIOSession session = sessionFactory.createSession(inbound);
                JettyWebSocketSessionAdapter adapter = new JettyWebSocketSessionAdapter(session);
                adapter.init(getServletConfig());
                wsFactory.upgrade(request, response, adapter, origin, protocol);
            }
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, this + " transport error: Invalid request");
        }
    }

    public void setBufferSize(int bufferSize) {
        wsFactory.setBufferSize(bufferSize);
    }

    public void setMaxIdleTime(int maxIdleTime) {
        wsFactory.setMaxIdleTime(maxIdleTime);
    }
}
