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
package com.glines.socketio.sample.eventbus;

import com.glines.socketio.common.DisconnectReason;
import com.glines.socketio.common.SocketIOException;
import com.glines.socketio.server.SocketIOInbound;
import com.glines.socketio.server.SocketIOOutbound;
import com.glines.socketio.server.SocketIOServlet;
import com.glines.socketio.util.JdkOverLog4j;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EventBusServlet extends SocketIOServlet {

    private static final Logger LOGGER = Logger.getLogger(EventBusServlet.class.getName());
    private static final long serialVersionUID = 5761989518185277878L;

    private final ConcurrentMap<String, Endpoints> subscriptions = new ConcurrentHashMap<String, Endpoints>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        JdkOverLog4j.install();
        super.init(config);
    }

    @Override
    protected SocketIOInbound doSocketIOConnect(HttpServletRequest request) {
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine("doSocketIOConnect : " + request.getRemoteHost() + ":" + request.getRemotePort());
        return new Endpoint(request.getSession().getId(), request.getRemoteHost(), request.getRemotePort());
    }

    private final class Endpoint implements SocketIOInbound {

        private final String remoteHost;
        private final int remotePort;
        private final String id;

        private volatile SocketIOOutbound outbound;

        private Endpoint(String id, String remoteHost, int remotePort) {
            this.remoteHost = remoteHost;
            this.remotePort = remotePort;
            this.id = id;
        }

        String getId() {
            return id;
        }

        @Override
        public String toString() {
            return "Endpoint " + id + " (" + remoteHost + ":" + remotePort + ")";
        }

        @Override
        public int hashCode() {
            return 31 * id.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Endpoint endpoint = (Endpoint) o;
            return id.equals(endpoint.id);
        }

        @Override
        public void onConnect(SocketIOOutbound outbound) {
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.fine(this + " connected.");
            this.outbound = outbound;
            try {
                send(new JSONObject().put("type", MessageType.ACK));
            } catch (JSONException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        @Override
        public void onDisconnect(DisconnectReason reason, String errorMessage) {
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.log(Level.FINE, this + " disconnected: reason=" + reason);
            this.outbound = null;
            for (Endpoints ee : subscriptions.values())
                ee.remove(this);
        }

        @Override
        public void onMessage(int messageType, String message) {
            if (outbound == null)
                throw new NullPointerException();
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.fine(this + " received message: " + message);
            try {
                JSONArray array = new JSONArray(message);
                for (int i = 0; i < array.length() && outbound != null; i++) {
                    JSONObject json = array.getJSONObject(i);
                    MessageType type = MessageType.valueOf(json.getInt("type"));
                    switch (type) {
                        case SUBSCRIBE: {
                            String topic = json.getString("topic");
                            if (LOGGER.isLoggable(Level.FINE))
                                LOGGER.log(Level.FINE, this + " subscribes to topic: " + topic);
                            subscriptions.putIfAbsent(topic, new Endpoints(topic));
                            subscriptions.get(topic).add(this);
                            break;
                        }
                        case UNSUBSCRIBE: {
                            String topic = json.getString("topic");
                            if (LOGGER.isLoggable(Level.FINE))
                                LOGGER.log(Level.FINE, this + " unsubscribes from topic: " + topic);
                            Endpoints ee = subscriptions.get(topic);
                            if (ee != null)
                                ee.remove(this);
                            return;
                        }
                        case PUBLISH: {
                            String topic = json.getString("topic");
                            String data = json.getString("data");
                            if (LOGGER.isLoggable(Level.FINE))
                                LOGGER.log(Level.FINE, this + " publishes to topic " + topic + " message: " + data);
                            Endpoints ee = subscriptions.get(topic);
                            if (ee != null)
                                ee.fire(topic, data);
                            break;
                        }
                        default: {
                            close();
                            throw new IllegalArgumentException("Illegal message: " + message);
                        }
                    }
                }
            } catch (JSONException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        void close() {
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.log(Level.FINE, this + " closing.");
            if (outbound != null) {
                outbound.close();
                this.outbound = null;
            }
        }

        void send(JSONObject data) {
            if (outbound != null) {
                String str = data.toString();
                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.fine("Sending to " + this + " message: " + str);
                try {
                    outbound.sendMessage(str);
                } catch (SocketIOException e) {
                    LOGGER.log(Level.SEVERE, "Error sending message to " + this + " => disconnecting. Error: " + e.getMessage(), e);
                    close();
                }
            }
        }
    }

    private final class Endpoints {

        final String topic;
        final ConcurrentMap<String, Endpoint> endpoints = new ConcurrentHashMap<String, Endpoint>();

        Endpoints(String topic) {
            this.topic = topic;
        }

        @Override
        public String toString() {
            return "Endpoints for " + topic + ": " + endpoints.size();
        }

        void add(Endpoint endpoint) {
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.log(Level.FINE, "Subscribing " + endpoint + " to " + this);
            Endpoint old = endpoints.get(endpoint.getId());
            if (old == null) {
                endpoints.putIfAbsent(endpoint.getId(), endpoint);
            } else {
                endpoints.replace(endpoint.getId(), old, endpoint);
            }
            if (old != null)
                old.close();
        }

        void remove(Endpoint endpoint) {
            if (endpoints.remove(endpoint.getId(), endpoint)) {
                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.log(Level.FINE, endpoint + " unsubscribed from " + this);
                if (endpoints.isEmpty()) {
                    subscriptions.remove(topic, this);
                    if (LOGGER.isLoggable(Level.FINE))
                        LOGGER.log(Level.FINE, this + " removed from subscriptions.");
                }
            }
        }

        void fire(String topic, String data) {
            for (Endpoint endpoint : endpoints.values())
                try {
                    endpoint.send(new JSONObject().put("type", MessageType.PUBLISH).put("topic", topic).put("data", data));
                } catch (JSONException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
        }
    }

    private static enum MessageType {

        ACK(4),
        SUBSCRIBE(1),
        UNSUBSCRIBE(2),
        PUBLISH(3),
        UNKNOWN(0);

        final int code;

        MessageType(int code) {
            this.code = code;
        }


        static MessageType valueOf(int code) {
            for (MessageType type : values())
                if (type.code == code)
                    return type;
            return UNKNOWN;
        }

        @Override
        public String toString() {
            return "" + code;
        }
    }

}
