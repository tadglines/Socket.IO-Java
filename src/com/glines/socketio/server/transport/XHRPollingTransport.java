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

package com.glines.socketio.server.transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationListener;
import org.eclipse.jetty.continuation.ContinuationSupport;

import com.glines.socketio.common.SocketIOMessage;
import com.glines.socketio.server.SocketIOClosedException;
import com.glines.socketio.server.SocketIOException;
import com.glines.socketio.server.SocketIOInbound;
import com.glines.socketio.server.SocketIOInbound.DisconnectReason;
import com.glines.socketio.server.SocketIOSession;
import com.glines.socketio.server.SocketIOSession.SessionTransportHandler;

public class XHRPollingTransport extends AbstractHttpTransport {
	public static final String TRANSPORT_NAME = "xhr-polling";
	public static long GET_TIMEOUT = 20*1000;
	private final int bufferSize;
	private final int maxIdleTime;

	public XHRPollingTransport(int bufferSize, int maxIdleTime) {
		this.bufferSize = bufferSize;
		this.maxIdleTime = maxIdleTime;
	}

	@Override
	public String getName() {
		return TRANSPORT_NAME;
	}

	interface SessionHelper extends SessionTransportHandler, TransportBuffer.BufferListener, ContinuationListener {
		void connect(HttpServletRequest request, HttpServletResponse response);
	}

	private void writeData(HttpServletResponse response, String data) throws IOException {
		response.setContentType("text/plain; charset=UTF-8");
		response.setContentLength(data.length());
		response.getWriter().write(data);
	}
	
	@Override
	protected SocketIOSession connect(
			HttpServletRequest request,
			HttpServletResponse response,
			SocketIOInbound.Factory inboundFactory,
			SocketIOSession.Factory sessionFactory) throws IOException {

		SocketIOInbound inbound = inboundFactory.getInbound(request, "");
		if (inbound != null) {
 			final SocketIOSession session = sessionFactory.createSession(inbound);
 			session.setTimeout(maxIdleTime/2);
			final SessionHelper handler =  new SessionHelper() {
				private final TransportBuffer buffer = new TransportBuffer(bufferSize);
				private volatile boolean isConnected = true;
				private Continuation continuation = null;

				@Override
				public void disconnect() {
					if (isConnected) {
						isConnected = false;
						if (buffer.putMessage(SocketIOMessage.encode(SocketIOMessage.Type.CLOSE, "close"), maxIdleTime) == false) {
							session.onDisconnect(DisconnectReason.TIMEOUT);
							abort();
						}
					}
				}
	
				@Override
				public boolean isOpen() {
					return isConnected;
				}
	
				@Override
				public void sendMessage(String message) throws SocketIOException {
					if (isConnected) {
						if (buffer.putMessage(message, maxIdleTime) == false) {
							session.onDisconnect(DisconnectReason.TIMEOUT);
							abort();
						}
					} else {
						throw new SocketIOClosedException();
					}
				}
	
				@Override
				public void handle(HttpServletRequest request,
						HttpServletResponse response, SocketIOSession session)
						throws IOException {
					if ("GET".equals(request.getMethod())) {
						session.clearTimeoutTimer();
						if (!isConnected && buffer.isEmpty()) {
							response.sendError(HttpServletResponse.SC_NOT_FOUND);
						} else {
							/*
							 * If there are messages in the buffer, get them and return them.
							 * If not, create continuation, register listener with buffer that will
							 * complete the continuation when a message arrives.
							 */
							List<String> messages = buffer.drainMessages();
							if (messages.size() > 0) {
								StringBuilder data = new StringBuilder();
								for (String msg: messages) {
									data.append(SocketIOMessage.encode(SocketIOMessage.Type.TEXT, msg));
								}
								writeData(response, data.toString());
								if (!isConnected) {
									session.onDisconnect(DisconnectReason.NORMAL);
									abort();
								} else {
									session.startTimeoutTimer();
								}
							} else {
								continuation = ContinuationSupport.getContinuation(request);
								continuation.setTimeout(20000);
								continuation.suspend(response);
								buffer.setListener(this);
							}
						}
					} else if ("POST".equals(request.getMethod())) {
						if (isConnected) {
							int size = request.getContentLength();
							BufferedReader reader = request.getReader();
							if (size == 0) {
								response.sendError(HttpServletResponse.SC_BAD_REQUEST);
							} else {
								String data;
								if (size > 0) {
									char[] buf = new char[size];
									if (reader.read(buf) != size) {
							    		response.sendError(HttpServletResponse.SC_BAD_REQUEST);
							    		return;
									}
									data = new String(buf);
								} else {
									StringBuilder str = new StringBuilder();
									char[] buf = new char[8192];
									int nread = 0;
									while ((nread = reader.read(buf)) != -1) {
										str.append(buf, 0, nread);
									}
									data = str.toString();
								}
								List<SocketIOMessage> list = SocketIOMessage.parse(data.substring(5));
								String origin = request.getHeader("Origin");
								if (origin != null) {
									// TODO: Verify origin
									
									// TODO: Explain why this is needed. Possibly for IE's XDomainRequest?
									response.setHeader("Access-Control-Allow-Origin", "*");
									if (request.getCookies() != null) {
										response.setHeader("Access-Control-Allow-Credentials", "true");
									}
								}
								for (SocketIOMessage msg: list) {
									session.onMessage(msg);
								}
							}
						}
					} else {
			    		response.sendError(HttpServletResponse.SC_BAD_REQUEST);
					}
				}

				@Override
				public boolean onMessage(String message) {
					return onMessages(Collections.singletonList(message));
				}

				@Override
				public boolean onMessages(List<String> messages) {
					try {
						buffer.setListener(null);
						HttpServletResponse response = (HttpServletResponse)continuation.getServletResponse();
						StringBuilder data = new StringBuilder();
						for (String msg: messages) {
							data.append(SocketIOMessage.encode(SocketIOMessage.Type.TEXT, msg));
						}
						try {
							writeData(response, data.toString());
						} catch (IOException e) {
							return false;
						}
						return true;
					} finally {
						continuation.complete();
					}
				}

				@Override
				public void onComplete(Continuation cont) {
					buffer.setListener(null);
					continuation = null;
					if (!isConnected && buffer.isEmpty()) {
						session.onDisconnect(DisconnectReason.NORMAL);
						abort();
					}
					session.startTimeoutTimer();
				}

				@Override
				public void onTimeout(Continuation cont) {
					buffer.setListener(null);
					continuation = null;
					if (!isConnected && buffer.isEmpty()) {
						session.onDisconnect(DisconnectReason.NORMAL);
						abort();
					}
				}

				@Override
				public void connect(HttpServletRequest request, HttpServletResponse response) {
					final StringBuilder str = new StringBuilder();
					str.append(SocketIOMessage.encode(SocketIOMessage.Type.SESSION_ID, session.getSessionId()));
					buffer.setListener(new TransportBuffer.BufferListener() {
						@Override
						public boolean onMessage(String message) {
							str.append(SocketIOMessage.encode(SocketIOMessage.Type.TEXT, message));
							return true;
						}
						@Override
						public boolean onMessages(List<String> messages) {
							for (String msg: messages) {
								str.append(SocketIOMessage.encode(SocketIOMessage.Type.TEXT, msg));
							}
							return true;
						}
					});
					session.onConnect(this);
					response.setContentType("text/plain; charset=UTF-8");
					String data = str.toString();
					response.setContentLength(data.length());
					try {
						response.getWriter().write(data);
					} catch (IOException e) {
						abort();
					}
				}

				@Override
				public void abort() {
					isConnected = false;
					// Drain buffer of all messages and release any threads that may have been blocked on it
					buffer.setListener(new TransportBuffer.BufferListener() {
						@Override
						public boolean onMessage(String message) {
							return false;
						}

						@Override
						public boolean onMessages(List<String> messages) {
							return false;
						}
					});
					buffer.clear();
					// cancel any continuation
					if (continuation != null) {
						try {
							((HttpServletResponse)continuation.getServletResponse()).sendError(HttpServletResponse.SC_NOT_FOUND);
						} catch (IOException e) {
							// Ignore since we are aborting anyway.
						}
						continuation.complete();
					}
					session.onShutdown();
				}
			};
			handler.connect(request, response);
			return session;
		}
		return null;
	}
}
