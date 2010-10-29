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

package com.glines.socketio.server;

import javax.servlet.http.HttpServletRequest;

import com.glines.socketio.common.CloseType;
import com.glines.socketio.common.ConnectionState;
import com.glines.socketio.common.DisconnectReason;
import com.glines.socketio.common.SocketIOException;
import com.glines.socketio.common.SocketIOMessageParser;

public interface SocketIOInbound {
	interface SocketIOOutbound {
		/**
		 * Terminate the connection. This method may return before the connection disconnect
		 * completes. The onDisconnect() method of the associated SocketInbound will be called
		 * when the disconnect is completed. The onDisconnect() method may be called during the
		 * invocation of this method.
		 */
		void disconnect();

		/**
		 * Initiate an orderly close of the connection. The state will be changed to CLOSING so no
		 * new messages can be sent, but messages may still arrive until the distent end has
		 * acknowledged the close.
		 * 
		 * @param closeType
		 */
		void close(CloseType closeType);
		
		ConnectionState getconnectionState();

		/**
		 * Send a message to the client. This method will block if the message will not fit in the
		 * outbound buffer.
		 * If the socket is closed, becomes closed, or times out, while trying to send the message,
		 * the SocketClosedException will be thrown.
		 *
		 * @param message The message to send
		 * @throws SocketIOException
		 */
		void sendMessage(String message) throws SocketIOException;
		
		/**
		 * Send a message.
		 * 
		 * @param message
		 * @throws IllegalStateException if the socket is not CONNECTED.
		 * @throws SocketIOMessageParserException if the message type parser encode() failed.
		 */
		void sendMessage(int messageType, Object message) throws SocketIOException;
	}

	/**
	 * Return the name of the protocol this inbound is associated with.
	 * This is one of the values provided by
	 * {@link SocketIOServlet#doSocketIOConnect(HttpServletRequest, String[])}.
	 * @return
	 */
	String getProtocol();
	
	/**
	 * Called when the connection is established. This will only ever be called once.
	 * @param outbound The SocketOutbound associated with the connection
	 */
	void onConnect(SocketIOOutbound outbound);
	
	/**
	 * Called when the socket connection is closed. This will only ever be called once.
	 * This method may be called instead of onConnect() if the connection handshake isn't
	 * completed successfully.
	 * @param reason The reason for the disconnect.
	 * @param errorMessage Possibly non null error message associated with the reason for disconnect.
	 */
	void onDisconnect(DisconnectReason reason, String errorMessage);
	
	/**
	 * Called if an orderly close completed.
	 * @param requestedType The type of close requested.
	 * @param result The type of close actually accomplished.
	 */
	void onClose(CloseType requestedType, CloseType result);


	/**
	 * Called one per arriving message.
	 * @param messageType
	 * @param message
	 * @param parseError
	 */
	void onMessage(int messageType, Object message, SocketIOException parseError);
	
	/**
	 * Return the parser associated with the requested message type, or null of non exists.
	 * This will only be called once per message type the first time that message type is
	 * encountered.
	 * @param messageType
	 * @return the parser or null
	 */
	SocketIOMessageParser getMessageParser(int messageType);
}
