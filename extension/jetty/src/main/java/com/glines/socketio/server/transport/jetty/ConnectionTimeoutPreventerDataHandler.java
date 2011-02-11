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
}
