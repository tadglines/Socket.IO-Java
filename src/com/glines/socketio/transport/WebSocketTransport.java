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

package com.glines.socketio.transport;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketFactory;

import com.glines.socketio.SocketIOException;
import com.glines.socketio.SocketIOInbound;
import com.glines.socketio.SocketIOSession;

public class WebSocketTransport extends AbstractTransport {
	public static final String TRANSPORT_NAME = "websocket";
	private final WebSocketFactory wsFactory;

	private class SessionWrapper implements WebSocket, SocketIOSession.SessionTransportHandler {
		private final SocketIOSession session;
		private Outbound outbound = null;
		private boolean connected = false;

		SessionWrapper(SocketIOSession session) {
			this.session = session;
		}

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.jetty.websocket.WebSocket#onConnect(org.eclipse.jetty.websocket.WebSocket.Outbound)
		 */
		@Override
		public void onConnect(final Outbound outbound) {
			this.outbound = outbound;
		}

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.jetty.websocket.WebSocket#onDisconnect()
		 */
		@Override
		public void onDisconnect() {
			if (connected) {
				session.onDisconnect(false);
			}
			outbound = null;
		}

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.jetty.websocket.WebSocket#onMessage(byte, java.lang.String)
		 */
		@Override
		public void onMessage(byte frame, String message) {
			if (!connected) {
				if ("~s~".equals(message)) {
					connected = true;
					try {
						outbound.sendMessage(encode(session.getSessionId()));
					} catch (IOException e) {
						outbound.disconnect();
					}
					session.onConnect(this);
				} else {
					outbound.disconnect();
				}
			} else {
				for(String msg: decode(message)) {
					session.onMessage(msg);
				}
			}
		}

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.jetty.websocket.WebSocket#onMessage(byte, byte[], int, int)
		 */
		@Override
		public void onMessage(byte frame, byte[] data, int offset, int length) {
            try
            {
                onMessage(frame,new String(data,offset,length,"UTF-8"));
            }
            catch(UnsupportedEncodingException e)
            {
            	// Do nothing for now.
            }
		}

		/*
		 * (non-Javadoc)
		 * @see com.glines.socketio.SocketIOInbound.SocketIOOutbound#disconnect()
		 */
		@Override
		public void disconnect() {
			outbound.disconnect();
		}

		/*
		 * (non-Javadoc)
		 * @see com.glines.socketio.SocketIOInbound.SocketIOOutbound#isOpen()
		 */
		@Override
		public boolean isOpen() {
			return outbound.isOpen();
		}

		/*
		 * (non-Javadoc)
		 * @see com.glines.socketio.SocketIOInbound.SocketIOOutbound#sendMessage(java.lang.String)
		 */
		@Override
		public void sendMessage(String message) throws SocketIOException {
			try {
				outbound.sendMessage(encode(message));
			} catch (IOException e) {
				throw new SocketIOException(e);
			}
		}

		/*
		 * (non-Javadoc)
		 * @see com.glines.socketio.SocketIOSession.SessionTransportHandler#handle(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, com.glines.socketio.SocketIOSession)
		 */
		@Override
		public void handle(HttpServletRequest request,
				HttpServletResponse response, SocketIOSession session) throws IOException {
    		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unexpected request on upgraded WebSocket connection");
    		return;
		}
		
	}

	public WebSocketTransport(int bufferSize, int maxIdleTime) {
		wsFactory = new WebSocketFactory();
		wsFactory.setBufferSize(bufferSize);
		wsFactory.setMaxIdleTime(maxIdleTime);
	}
	
	@Override
	public String getName() {
		return TRANSPORT_NAME;
	}
	
	@Override
	public void handle(HttpServletRequest request,
			HttpServletResponse response,
			SocketIOInbound.Factory inboundFactory,
			SocketIOSession.Factory sessionFactory)
			throws IOException {

		String sessionId = extractSessionId(request);
		
		if ("GET".equals(request.getMethod()) && sessionId == null && "WebSocket".equals(request.getHeader("Upgrade"))) {
			boolean hixie = request.getHeader("Sec-WebSocket-Key1") != null;
            
            String protocol=request.getHeader(hixie ? "Sec-WebSocket-Protocol" : "WebSocket-Protocol");
            if (protocol == null)
                protocol=request.getHeader("Sec-WebSocket-Protocol");

	        String host=request.getHeader("Host");
	        String origin=request.getHeader("Origin");
	        if (origin == null) {
	        	origin = host;
	        }
	
	        SocketIOInbound inbound = inboundFactory.getInbound(request, protocol);
	        if (inbound == null) {
	        	if (hixie) {
                    response.setHeader("Connection","close");
	        	}
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
	        } else {
	        	SocketIOSession session = sessionFactory.createSession(inbound);
		        SessionWrapper wrapper = new SessionWrapper(session);
		        
		        wsFactory.upgrade(request,response,wrapper,origin,protocol);
	        }
		} else {
    		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid " + TRANSPORT_NAME + " transport request");
		}
	}
}
