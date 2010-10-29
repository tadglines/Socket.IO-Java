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

package com.glines.socketio.examples.chat;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.Cookie;

import org.eclipse.jetty.util.ajax.JSON;

import com.glines.socketio.common.CloseType;
import com.glines.socketio.common.DisconnectReason;
import com.glines.socketio.common.SocketIOException;
import com.glines.socketio.common.SocketIOMessageParser;
import com.glines.socketio.server.SocketIOInbound;
import com.glines.socketio.server.SocketIOServlet;

public class ChatSocketServlet extends SocketIOServlet {
	private static final long serialVersionUID = 1L;
	private AtomicInteger ids = new AtomicInteger(1);
	private Set<ChatConnection> connections = new HashSet<ChatConnection>();

	private class ChatConnection implements SocketIOInbound {
		private SocketIOOutbound outbound = null;
		private Integer sessionId = ids.getAndIncrement();

		@Override
		public String getProtocol() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void onConnect(SocketIOOutbound outbound) {
			this.outbound = outbound;
			synchronized (connections) {
				connections.add(this);
			}
			try {
				outbound.sendMessage("~j~" + JSON.toString(
						Collections.singletonMap("buffer", new String[]{})));
			} catch (SocketIOException e) {
				outbound.disconnect();
			}
			broadcast("~j~" + JSON.toString(
					Collections.singletonMap("announcement", sessionId + " connected")));
		}

		@Override
		public void onDisconnect(DisconnectReason reason, String errorMessage) {
			synchronized(this) {
				this.outbound = null;
			}
			synchronized (connections) {
				connections.remove(this);
			}
			broadcast("~j~" + JSON.toString(
					Collections.singletonMap("announcement", sessionId + " disconnected")));
		}

		@Override
		public void onClose(CloseType requestedType, CloseType result) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onMessage(int messageType, Object message,
				SocketIOException parseError) {
			System.out.println("Recieved: " + message);
			broadcast("~j~" + JSON.toString(
					Collections.singletonMap("message",
							new String[]{sessionId.toString(), (String)message})));
		}

		private void broadcast(String message) {
			System.out.println("Broadcasting: " + message);
			synchronized (connections) {
				for(ChatConnection c: connections) {
					if (c != this) {
						try {
							c.outbound.sendMessage(message);
						} catch (IOException e) {
							c.outbound.disconnect();
						}
					}
				}
			}
		}

		@Override
		public SocketIOMessageParser getMessageParser(int messageType) {
			// TODO Auto-generated method stub
			return null;
		}
	}

	@Override
	protected SocketIOInbound doSocketIOConnect(Cookie[] cookies, String host,
			String origin, String[] protocols) {
		return new ChatConnection();
	}

}
