package com.glines.socketio.transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationListener;
import org.eclipse.jetty.continuation.ContinuationSupport;

import com.glines.socketio.SocketIOException;
import com.glines.socketio.SocketIOInbound;
import com.glines.socketio.SocketIOSession;
import com.glines.socketio.SocketIOSession.SessionTransportHandler;

public class XHRPollingTransport extends AbstractHttpTransport {
	public static final String TRANSPORT_NAME = "xhr-polling";
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
			final SessionHelper handler =  new SessionHelper() {
				private final TransportBuffer buffer = new TransportBuffer(bufferSize);
				private boolean isConnected = true;
				private Continuation continuation = null;

				@Override
				public void disconnect() {
					buffer.setListener(null);
					isConnected = false;
					if (continuation != null) {
						HttpServletResponse response = (HttpServletResponse)continuation.getServletResponse();
						response.setHeader("Connection", "close");
						continuation.complete();
					}
				}
	
				@Override
				public boolean isOpen() {
					return isConnected;
				}
	
				@Override
				public void sendMessage(String message) throws SocketIOException {
					if (buffer.putMessage(message, maxIdleTime) == false) {
						session.onDisconnect(true);
					}
				}
	
				@Override
				public void handle(HttpServletRequest request,
						HttpServletResponse response, SocketIOSession session)
						throws IOException {
					if ("GET".equals(request.getMethod())) {
						if (isConnected == false) {
							response.setHeader("Connection", "close");
						} else {
							/*
							 * If there are messages in the buffer, get them and return them.
							 * If not, create continuation, register listener with buffer that will
							 * complete the continuation when a message arrives.
							 */
							List<String> messages = buffer.drainMessages();
							if (messages.size() > 0) {
								writeData(response, encode(messages));
							} else {
								continuation = ContinuationSupport.getContinuation(request);
								continuation.setTimeout(20000);
								continuation.suspend(response);
								buffer.setListener(this);
							}
						}
					} else if ("POST".equals(request.getMethod())) {
						int size = request.getContentLength();
						BufferedReader reader = request.getReader();
						String data;
						List<String> list;
						if (size == 0) {
							list = Collections.singletonList("");
						} else {
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
							list = decode(data.substring(5));
						}
						for (String msg: list) {
							try {
								session.onMessage(msg);
							} catch (Throwable t) {
								// Ignore
							}
						}
					} else {
			    		response.sendError(HttpServletResponse.SC_BAD_REQUEST);
					}
				}

				@Override
				public boolean onMessage(String message) {
					try {
						buffer.setListener(null);
						HttpServletResponse response = (HttpServletResponse)continuation.getServletResponse();
						try {
							writeData(response, encode(message));
						} catch (IOException e) {
							return false;
						}
						return true;
					} finally {
						continuation.complete();
					}
				}

				@Override
				public boolean onMessages(List<String> messages) {
					try {
						buffer.setListener(null);
						HttpServletResponse response = (HttpServletResponse)continuation.getServletResponse();
						try {
							writeData(response, encode(messages));
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
					if (isConnected != true) {
						session.onDisconnect(false);
					}
					continuation = null;
				}

				@Override
				public void onTimeout(Continuation cont) {
					// TODO Auto-generated method stub
					buffer.setListener(null);
					continuation = null;
				}

				@Override
				public void connect(HttpServletRequest request, HttpServletResponse response) {
					buffer.putMessage(session.getSessionId(), 0);
					continuation = ContinuationSupport.getContinuation(request);
					continuation.suspend(response);
					buffer.setListener(this);
					session.onConnect(this);
					buffer.flush();
				}
			};
			handler.connect(request, response);
			return session;
		}
		return null;
	}
}
