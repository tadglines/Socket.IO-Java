package com.glines.socketio.server;

import javax.servlet.ServletConfig;

/**
 * @author Mathieu Carbou
 */
public abstract class AbstractSessionTransportHandler implements SessionTransportHandler {

    private ServletConfig servletConfig;
    private SocketIOConfig config;

    @Override
    public final void init(ServletConfig config) throws SessionTransportInitializationException {
        this.servletConfig = config;
        this.config = new ServletBasedSocketIOConfig(servletConfig);
        init();
    }

    protected final ServletConfig getServletConfig() {
        return servletConfig;
    }

    protected final SocketIOConfig getConfig() {
        return config;
    }

    protected void init() throws SessionTransportInitializationException {}

    @Override
    public void disconnectWhenEmpty() {}
}
