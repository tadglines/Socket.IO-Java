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

import com.glines.socketio.server.SocketIOConfig;
import com.glines.socketio.server.transport.DataHandler;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Mathieu Carbou
 */
final class ConnectionTimeoutPreventerDataHandler implements DataHandler {

    private final DataHandler delegate;
    private final ConnectionTimeoutPreventer timeoutPreventer;

    public ConnectionTimeoutPreventerDataHandler(DataHandler delegate, ConnectionTimeoutPreventer timeoutPreventer) {
        this.delegate = delegate;
        this.timeoutPreventer = timeoutPreventer;
    }

    @Override
    public void init(SocketIOConfig config) {
        delegate.init(config);
    }

    @Override
    public boolean isConnectionPersistent() {
        return delegate.isConnectionPersistent();
    }

    @Override
    public void onConnect(HttpServletRequest request, HttpServletResponse response) throws IOException {
        timeoutPreventer.connectionActive();
        delegate.onConnect(request, response);
    }

    @Override
    public void onFinishSend(ServletResponse response) throws IOException {
        delegate.onFinishSend(response);
    }

    @Override
    public void onStartSend(HttpServletResponse response) throws IOException {
        delegate.onStartSend(response);
    }

    @Override
    public void onWriteData(ServletResponse response, String data) throws IOException {
        timeoutPreventer.connectionActive();
        delegate.onWriteData(response, data);
    }

    @Override
    public String toString() {
        return "withTimeoutPreventer(" + delegate + ")";
    }
}
