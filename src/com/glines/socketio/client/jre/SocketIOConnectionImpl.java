/**
 * The MIT License
 * Copyright (c) 2010 Tad Glines
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

package com.glines.socketio.client.jre;

import org.eclipse.jetty.client.HttpClient;

import com.glines.socketio.client.common.SocketIOConnection;
import com.glines.socketio.common.ConnectionState;
import com.glines.socketio.common.DisconnectReason;
import com.glines.socketio.common.SocketIOException;

public class SocketIOConnectionImpl implements SocketIOConnection {
	private final SocketIOConnection.SocketIOConnectionListener listener;
	private final String host;
	private final short port;
	private HttpClient client;

	public SocketIOConnectionImpl(SocketIOConnection.SocketIOConnectionListener listener, 
			String host, short port) {
		this.listener = listener;
		this.host = host;
		this.port = port;
	}
	
	@Override
	public void connect() {
		if (client == null) {
			client = new HttpClient();
			client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
			try {
				client.start();
			} catch (Exception e) {
				client = null;
				listener.onDisconnect(DisconnectReason.ERROR, "Failed to initialize");
			}
		}
		
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void disconnect() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ConnectionState getConnectionState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void sendMessage(String message) throws SocketIOException {
		sendMessage(0, message);
	}

	@Override
	public void sendMessage(int messageType, String message) throws SocketIOException {
		// TODO Auto-generated method stub
		
	}
}
