package com.glines.socketio.server;

import java.util.Collection;

/**
 * @author Mathieu Carbou
 */
public interface SocketIOConfig {

    String PARAM_HEARTBEAT_DELAY = "heartbeat-delay";
    String PARAM_HEARTBEAT_TIMEOUT = "heartbeat-timeout";
    String PARAM_TIMEOUT = "timeout";

    String PARAM_BUFFER_SIZE = "bufferSize";
    String PARAM_MAX_IDLE = "maxIdleTime";

    int DEFAULT_BUFFER_SIZE = 8192;
    int DEFAULT_MAX_IDLE = 300 * 1000;

    long getHeartbeatDelay(long def);
    long getHeartbeatTimeout(long def);
    long getTimeout(long def);
    int getBufferSize();
    int getMaxIdle();

    void addTransport(Transport transport);
    Collection<Transport> getTransports();
    Transport getTransport(TransportType type);
    void removeTransport(TransportType type);
    Transport getWebSocketTransport();

    String getString(String key);
    String getString(String key, String def);
    int getInt(String key, int def);
    long getLong(String key, long def);

    String getNamespace();
}
