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
package com.glines.socketio.server;

import javax.servlet.ServletConfig;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mathieu Carbou
 */
final class ServletBasedSocketIOConfig implements SocketIOConfig {

    private static final Logger LOGGER = Logger.getLogger(ServletBasedSocketIOConfig.class.getName());
    private static final String KEY_TRANSPORT = SocketIOConfig.class.getName() + ".TRANSPORTS";

    private final ServletConfig config;
    private final String namespace;

    public ServletBasedSocketIOConfig(ServletConfig config, String namespace) {
        this.namespace = namespace;
        this.config = config;
    }

    @Override
    public long getHeartbeatDelay(long def) {
        return getLong(PARAM_HEARTBEAT_DELAY, def);
    }

    @Override
    public long getHeartbeatTimeout(long def) {
        return getLong(PARAM_HEARTBEAT_TIMEOUT, def);
    }

    @Override
    public long getTimeout(long def) {
        return getLong(PARAM_TIMEOUT, def);
    }

    @Override
    public int getBufferSize() {
        return getInt(PARAM_BUFFER_SIZE, DEFAULT_BUFFER_SIZE);
    }

    @Override
    public int getMaxIdle() {
        return getInt(PARAM_MAX_IDLE, DEFAULT_MAX_IDLE);
    }

    @Override
    public void addTransport(Transport transport) {
        getTransportMap().put(transport.getType(), transport);
    }

    @Override
    public Collection<Transport> getTransports() {
        return getTransportMap().values();
    }

    @Override
    public Transport getTransport(TransportType type) {
        return getTransportMap().get(type);
    }

    @Override
    public void removeTransport(TransportType type) {
        getTransportMap().remove(type);
    }

    @Override
    public Transport getWebSocketTransport() {
        return getTransport(TransportType.WEB_SOCKET);
    }

    @Override
    public int getInt(String param, int def) {
        String v = getString(param);
        return v == null ? def : Integer.parseInt(v);
    }

    @Override
    public long getLong(String param, long def) {
        String v = getString(param);
        return v == null ? def : Long.parseLong(v);
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public String getString(String param) {
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine("Getting InitParameter: " + namespace + "." + param);
        String v = config.getInitParameter(namespace + "." + param);
        if (v == null) {
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.fine("Fallback to InitParameter: " + param);
            v = config.getInitParameter(param);
        }
        return v;
    }

    @Override
    public String getString(String param, String def) {
        String v = getString(param);
        return v == null ? def : v;
    }

    @SuppressWarnings({"unchecked"})
    private Map<TransportType, Transport> getTransportMap() {
        Map<TransportType, Transport> transports = (Map<TransportType, Transport>) config.getServletContext().getAttribute(KEY_TRANSPORT);
        if (transports == null)
            config.getServletContext().setAttribute(KEY_TRANSPORT, transports = new ConcurrentHashMap<TransportType, Transport>());
        return transports;
    }

}
