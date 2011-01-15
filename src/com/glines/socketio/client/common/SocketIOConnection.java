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

package com.glines.socketio.client.common;

import com.glines.socketio.common.ConnectionState;
import com.glines.socketio.common.DisconnectReason;
import com.glines.socketio.common.SocketIOException;

public interface SocketIOConnection {
	interface Factory {
		SocketIOConnection create(SocketIOConnection.SocketIOConnectionListener listener,
				String host, short port);
	}

	interface SocketIOConnectionListener {
		public abstract void onConnect();
		public abstract void onDisconnect(DisconnectReason reason, String errorMessage);
		public abstract void onMessage(int messageType, String message);
	}

	/**
	 * Initiate a connection attempt. If the connection succeeds, then the
	 * {@link SocketIOConnectionListener#onConnect() onConnect} will be called. If the connection
	 * attempt fails, then
	 * {@link SocketIOConnectionListener#onDisconnect(DisconnectReason, String) onDisonnect} will
	 * be called.
	 * @throws IllegalStateException if the socket is not CLOSED.
	 */
	void connect();

	/**
	 * Forcefully disconnect the connection, discarding any unsent messages.
	 * This does nothing if the connection is already CLOSED.
	 * This will abort an orderly close if one was initiated.
	 */
	void disconnect();

	/**
	 * Initiate an orderly close of the connection.
	 * 
	 * @throws IllegalStateException if the socket is not CONNECTED.
	 */
	void close();
	
	/**
	 * Return the current socket connection state.
	 * @return
	 */
	ConnectionState getConnectionState();

	/**
	 * Send a message.
	 * 
	 * @param message
	 * @throws IllegalStateException if the socket is not CONNECTED.
	 */
	void sendMessage(String message) throws SocketIOException;

	/**
	 * Send a message. With a default priority of 0.
	 * 
	 * @param message
	 * @throws IllegalStateException if the socket is not CONNECTED.
	 * @throws SocketIOMessageParserException if the message type parser encode() failed.
	 */
	void sendMessage(int messageType, String message) throws SocketIOException;
}
