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

import com.glines.socketio.server.*;
import com.glines.socketio.util.Web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public abstract class AbstractHttpTransport extends AbstractTransport {

    public static final String SESSION_KEY = AbstractHttpTransport.class.getName() + ".Session";

    @Override
    public final void handle(HttpServletRequest request,
                             HttpServletResponse response,
                             Transport.InboundFactory inboundFactory,
                             SessionManager sessionFactory) throws IOException {

        Object obj = request.getAttribute(SESSION_KEY);
        SocketIOSession session = null;
        String sessionId = null;
        if (obj != null) {
            session = (SocketIOSession) obj;
        } else {
            sessionId = Web.extractSessionId(request);
            if (sessionId != null && sessionId.length() > 0) {
                session = sessionFactory.getSession(sessionId);
            }
        }
        if (session != null) {
            TransportHandler handler = session.getTransportHandler();
            if (handler != null) {
                handler.handle(request, response, session);
            } else {
                session.onShutdown();
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } else if (sessionId != null && sessionId.length() > 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        } else {
            if ("GET".equals(request.getMethod())) {
                session = connect(request, response, inboundFactory, sessionFactory);
                if (session == null) {
                    response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                }
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
        }
    }

    private SocketIOSession connect(HttpServletRequest request,
                                    HttpServletResponse response,
                                    InboundFactory inboundFactory,
                                    SessionManager sessionFactory) throws IOException {
        SocketIOInbound inbound = inboundFactory.getInbound(request);
        if (inbound != null) {
            SocketIOSession session = sessionFactory.createSession(inbound);
            DataHandler dataHandler = newDataHandler(session);
            dataHandler.init(getConfig());
            TransportHandler transportHandler = newHandler(ConnectableTransportHandler.class, session);
            ConnectableTransportHandler connectableTransportHandler = ConnectableTransportHandler.class.cast(transportHandler);
            connectableTransportHandler.setDataHandler(dataHandler);
            connectableTransportHandler.connect(request, response);
            transportHandler.init(getConfig());
            return session;
        }
        return null;
    }

    protected abstract DataHandler newDataHandler(SocketIOSession session);
}
