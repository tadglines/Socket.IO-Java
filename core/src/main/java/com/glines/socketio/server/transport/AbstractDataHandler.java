package com.glines.socketio.server.transport;

import com.glines.socketio.server.SocketIOConfig;

/**
 * @author Mathieu Carbou
 */
abstract class AbstractDataHandler implements DataHandler {

    private SocketIOConfig config;

    @Override
    public final void init(SocketIOConfig config) {
        this.config = config;
        init();
    }

    protected final SocketIOConfig getConfig() {
        return config;
    }

    protected void init() {
    }

}
