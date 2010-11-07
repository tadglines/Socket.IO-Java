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

package com.glines.socketio.examples.broadcast;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.glines.socketio.common.DisconnectReason;
import com.glines.socketio.server.SocketIOInbound;
import com.glines.socketio.server.SocketIOServlet;

public class BroadcastSocketServlet extends SocketIOServlet {
	private static final long serialVersionUID = 1L;
	private Set<BroadcastConnection> connections = new HashSet<BroadcastConnection>();

	private class BroadcastConnection implements SocketIOInbound {
		private SocketIOOutbound outbound = null;

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
		}

		@Override
		public void onDisconnect(DisconnectReason reason, String errorMessage) {
			synchronized(this) {
				this.outbound = null;
			}
			synchronized (connections) {
				connections.remove(this);
			}
		}

		@Override
		public void onMessage(int messageType, String message) {
			broadcast(messageType, message);
		}

		private void broadcast(int messageType, String message) {
			System.out.println("Broadcasting: " + message);
			synchronized (connections) {
				for(BroadcastConnection c: connections) {
					if (c != this) {
						try {
							c.outbound.sendMessage(messageType, message);
						} catch (IOException e) {
							c.outbound.disconnect();
						}
					}
				}
			}
		}
	}

	@Override
	protected SocketIOInbound doSocketIOConnect(HttpServletRequest request, String[] protocols) {
		return new BroadcastConnection();
	}

}
