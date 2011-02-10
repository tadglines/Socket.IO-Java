package com.glines.socketio.server;

/**
 * @author Mathieu Carbou
 */
public interface TransportDiscovery {
    Iterable<Transport> discover();
}
