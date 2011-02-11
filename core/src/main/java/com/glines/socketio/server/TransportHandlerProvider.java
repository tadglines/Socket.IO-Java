package com.glines.socketio.server;

import java.util.Collection;
import java.util.Map;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public interface TransportHandlerProvider {
    TransportHandler get(Class<?> handlerType, TransportType transportType);
    boolean isSupported(TransportType type);

    Map<TransportType, Class<TransportHandler>> listAll();

    void init();
}
