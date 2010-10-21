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

package com.glines.socketio;

import javax.servlet.http.HttpServletRequest;

public interface SocketIOInbound {
	interface Factory {
		SocketIOInbound getInbound(HttpServletRequest request, String protocol);
	}
	
	interface SocketIOOutbound {
		/**
		 * Initiate socket disconnect. This method may return before the connection disconnect completes.
		 * The onDisconnect() method of the associated SocketInbound will be called when the disconnect is completed.
		 * The onDisconnect() method may be called during the invocation of this method.
		 */
		void disconnect();

		/**
		 * @return true if connection is open
		 */
		boolean isOpen();

		/**
		 * Send a message to the client. This method will block if the message will not fit in the outbound buffer.
		 * If the socket is closed, becomes closed, or times out, while trying to send the message,
		 * the SocketClosedException will be thrown.
		 *
		 * @param message The message to send
		 * @throws SocketIOException
		 */
		void sendMessage(String message) throws SocketIOException;
	}

	/**
	 * Called when the connection is established. This will only ever be called once.
	 * @param outbound The SocketOutbound associated with the connection
	 */
	void onConnect(SocketIOOutbound outbound);
	
	/**
	 * Called when the socket connection is closed. This will only ever be called once.
	 * @param timeout this value will be true if the connection was closed due to a timeout
	 */
	void onDisconnect(boolean timeout);
	
	/**
	 * Called once for each message sent by the client.
	 * @param message the message sent by the client
	 */
	void onMessage(String message);
}
