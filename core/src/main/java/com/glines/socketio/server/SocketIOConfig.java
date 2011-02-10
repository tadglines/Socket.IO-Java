package com.glines.socketio.server;

import java.util.Collection;

/**
 * @author Mathieu Carbou
 */
public interface SocketIOConfig {

    String KEY_TRANSPORT_WEBSOCKET = "websocket";

    String PARAM_BUFFER_SIZE = "bufferSize";
    String PARAM_MAX_IDLE = "maxIdleTime";
    String PARAM_HEARTBEAT = "heartbeat";
    String PARAM_SESSION_TIMEOUT = "timeout";

    String PARAM_FLASHPOLICY_DOMAIN = "flashPolicyDomain";
    String PARAM_FLASHPOLICY_SERVER_HOST = "flashPolicyServerHost";
    String PARAM_FLASHPOLICY_SERVER_PORT = "flashPolicyServerPort";
    String PARAM_FLASHPOLICY_PORTS = "flashPolicyPorts";

    int DEFAULT_BUFFER_SIZE = 8192;
    int DEFAULT_MAX_IDLE = 300 * 1000;
    int DEFAULT_HEARTBEAT = DEFAULT_MAX_IDLE / 2;
    int DEFAULT_SESSION_TIMEOUT = 10 * 1000;

    long getHeartbeat();

    long getTimeout();

    int getBufferSize();

    int getMaxIdle();

    void addTransport(Transport transport);

    Collection<Transport> getTransports();

    Transport getTransport(String name);

    void removeTransport(String name);

    Transport getWebSocketTransport();

    String getFlashPolicyDomain();

    String getFlashPolicyPorts();

    String getFlashPolicyServerHost();

    int getFlashPolicyServerPort();
}
