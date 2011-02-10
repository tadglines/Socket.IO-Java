package com.glines.socketio.server;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Mathieu Carbou
 */
public class TransportAdapter implements Transport {
    @Override
    public void destroy() {
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void init(ServletConfig config) {
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, InboundFactory inboundFactory, SocketIOSession.Factory sessionFactory) throws IOException {
    }
}
