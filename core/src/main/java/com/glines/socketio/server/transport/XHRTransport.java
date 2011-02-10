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

import com.glines.socketio.server.SocketIOInbound;
import com.glines.socketio.server.SocketIOSession;
import com.glines.socketio.server.Transport;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

abstract class XHRTransport extends HttpTransport {

    /**
     * This method should only be called within the context of an active HTTP request.
     */
    protected abstract JettyXHRSessionHelper createHelper(SocketIOSession session);

    @Override
    protected final SocketIOSession connect(HttpServletRequest request,
                                      HttpServletResponse response, Transport.InboundFactory inboundFactory,
                                      com.glines.socketio.server.SocketIOSession.Factory sessionFactory)
            throws IOException {
        SocketIOInbound inbound = inboundFactory.getInbound(request);
        if (inbound != null) {
            SocketIOSession session = sessionFactory.createSession(inbound);
            JettyXHRSessionHelper handler = createHelper(session);
            handler.init(getServletConfig());
            handler.connect(request, response);
            return session;
        }
        return null;
    }

}
