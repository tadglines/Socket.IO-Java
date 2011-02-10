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

/**
 * @author Mathieu Carbou
 */
final class ServletBasedSocketIOConfig implements SocketIOConfig {

    private static final String KEY_TRANSPORT = SocketIOConfig.class.getName() + ".TRANSPORTS";

    private final ServletConfig config;

    public ServletBasedSocketIOConfig(ServletConfig config) {
        this.config = config;
    }

    @Override
    public long getHeartbeat() {
        return getInt(PARAM_HEARTBEAT, DEFAULT_HEARTBEAT);
    }

    @Override
    public long getTimeout() {
        return getInt(PARAM_SESSION_TIMEOUT, DEFAULT_SESSION_TIMEOUT);
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
        getTransportMap().put(transport.getName(), transport);
    }

    @Override
    public Collection<Transport> getTransports() {
        return getTransportMap().values();
    }

    @Override
    public Transport getTransport(String name) {
        return getTransportMap().get(name);
    }

    @Override
    public void removeTransport(String name) {
        getTransportMap().remove(name);
    }

    @Override
    public Transport getWebSocketTransport() {
        return getTransport(KEY_TRANSPORT_WEBSOCKET);
    }

    @Override
    public String getFlashPolicyDomain() {
        return config.getInitParameter(PARAM_FLASHPOLICY_DOMAIN);
    }

    @Override
    public String getFlashPolicyPorts() {
        return config.getInitParameter(PARAM_FLASHPOLICY_PORTS);
    }

    @Override
    public String getFlashPolicyServerHost() {
        return config.getInitParameter(PARAM_FLASHPOLICY_SERVER_HOST);
    }

    @Override
    public int getFlashPolicyServerPort() {
        return getInt(PARAM_FLASHPOLICY_SERVER_PORT, 843);
    }

    private  int getInt(String param, int def) {
        String v = config.getInitParameter(param);
        return v == null ? def : Integer.parseInt(v);
    }

    @SuppressWarnings({"unchecked"})
    private Map<String, Transport> getTransportMap() {
        Map<String, Transport> transports = (Map<String, Transport>) config.getServletContext().getAttribute(KEY_TRANSPORT);
        if (transports == null)
            config.getServletContext().setAttribute(KEY_TRANSPORT, transports = new ConcurrentHashMap<String, Transport>());
        return transports;
    }

}
