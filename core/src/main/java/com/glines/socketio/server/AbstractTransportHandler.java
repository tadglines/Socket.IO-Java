package com.glines.socketio.server;

/**
 * @author Mathieu Carbou
 */
public abstract class AbstractTransportHandler implements TransportHandler {

    private SocketIOConfig config;
    private SocketIOSession session;

    @Override
    public final void init(SocketIOConfig config) {
        this.config = config;
        init();
    }

    @Override
    public void setSession(SocketIOSession session) {
        this.session = session;
    }

    protected final SocketIOConfig getConfig() {
        return config;
    }

    protected final SocketIOSession getSession() {
        return session;
    }

    protected void init() {
    }

    @Override
    public void disconnectWhenEmpty() {
    }
}
