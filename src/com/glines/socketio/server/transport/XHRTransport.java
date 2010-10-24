package com.glines.socketio.server.transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationListener;
import org.eclipse.jetty.continuation.ContinuationSupport;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.URIUtil;

import com.glines.socketio.common.SocketIOMessage;
import com.glines.socketio.server.SocketIOClosedException;
import com.glines.socketio.server.SocketIOException;
import com.glines.socketio.server.SocketIOInbound;
import com.glines.socketio.server.SocketIOSession;
import com.glines.socketio.server.SocketIOInbound.DisconnectReason;
import com.glines.socketio.server.SocketIOInbound.Factory;
import com.glines.socketio.server.SocketIOSession.SessionTransportHandler;

public abstract class XHRTransport extends AbstractHttpTransport {
	public static final String CONTINUATION_KEY = "com.glines.socketio.server.transport.XHRTransport.Continuation";
	private final int bufferSize;
	private final int maxIdleTime;

	protected abstract class XHRSessionHelper implements SessionTransportHandler, ContinuationListener {
		protected final SocketIOSession session;
		private final TransportBuffer buffer = new TransportBuffer(bufferSize);
		private boolean is_open = false;
		private Continuation continuation = null;
		private final boolean isConnectionPersistant;

		XHRSessionHelper(SocketIOSession session, boolean isConnectionPersistant) {
			this.session = session;
			this.isConnectionPersistant = isConnectionPersistant;
			if (isConnectionPersistant) {
				session.setHeartbeat(HEARTBEAT_DELAY);
				session.setTimeout(HEARTBEAT_TIMEOUT);
			} else {
				session.setTimeout((HTTP_REQUEST_TIMEOUT-REQUEST_TIMEOUT)/2);
			}
		}

		protected abstract void startSend(HttpServletResponse response) throws IOException;

		protected abstract void writeData(ServletResponse response, String data) throws IOException;

		protected abstract void finishSend(ServletResponse response) throws IOException;
		
		@Override
		public void disconnect() {
			try {
				sendMessage(SocketIOMessage.Type.CLOSE, "close");
			} catch (SocketIOException e) {
				abort();
			}
		}

		@Override
		public boolean isOpen() {
			return is_open;
		}

		@Override
		public void sendMessage(SocketIOMessage.Type type, String message) throws SocketIOException {
			if (is_open) {
				System.out.println("Session["+session.getSessionId()+"]: sendMessage: [" + type + "]: " + message);
				if (continuation != null && continuation.isInitial()) {
					List<String> messages = buffer.drainMessages();
					messages.add(SocketIOMessage.encode(type, message));
					if (messages.size() > 0) {
						StringBuilder data = new StringBuilder();
						for (String msg: messages) {
							data.append(msg);
						}
						try {
							writeData(continuation.getServletResponse(), data.toString());
							session.startHeartbeatTimer();
						} catch (IOException e) {
							throw new SocketIOException(e);
						}
					}
				} else {
					String data = SocketIOMessage.encode(type, message);
					if (continuation != null && continuation.isSuspended() && buffer.getAvailableBytes() < data.length()) {
						continuation.resume();
					}
					if (buffer.putMessage(data, maxIdleTime) == false) {
						session.onDisconnect(DisconnectReason.TIMEOUT);
						abort();
						throw new SocketIOException();
					}
					if (continuation != null && continuation.isSuspended()) {
						continuation.resume();
					}
				}
			} else {
				throw new SocketIOClosedException();
			}
		}

		@Override
		public void sendMessage(SocketIOMessage message) throws SocketIOException {
			sendMessage(message.getType(), message.getData());
		}

		@Override
		public void sendMessage(String message) throws SocketIOException {
			sendMessage(SocketIOMessage.Type.TEXT, message);
		}

		@Override
		public void handle(HttpServletRequest request,
				HttpServletResponse response, SocketIOSession session)
				throws IOException {
			if ("GET".equals(request.getMethod())) {
				if (!is_open && buffer.isEmpty()) {
					response.sendError(HttpServletResponse.SC_NOT_FOUND);
				} else {
					Continuation cont = (Continuation)request.getAttribute(CONTINUATION_KEY);
					if (cont != null && cont != continuation) {
						return;
					}
					if (continuation != null) {
						List<String> messages = buffer.drainMessages();
						if (messages.size() > 0) {
							StringBuilder data = new StringBuilder();
							for (String msg: messages) {
								data.append(msg);
							}
							writeData(continuation.getServletResponse(), data.toString());
							if (isConnectionPersistant) {
								continuation.suspend(response);
							} else {
								finishSend(response);
								request.removeAttribute(CONTINUATION_KEY);
							}
							session.startHeartbeatTimer();
						} else {
							continuation.suspend(response);
						}
					} else if (!isConnectionPersistant) {
						if (!buffer.isEmpty()) {
							List<String> messages = buffer.drainMessages();
							if (messages.size() > 0) {
								StringBuilder data = new StringBuilder();
								for (String msg: messages) {
									data.append(msg);
								}
								startSend(response);
								writeData(response, data.toString());
								finishSend(response);
								session.startTimeoutTimer();
							}
						} else {
							session.clearTimeoutTimer();
							request.setAttribute(SESSION_KEY, session);
							continuation = ContinuationSupport.getContinuation(request);
							continuation.addContinuationListener(this);
							continuation.setTimeout(REQUEST_TIMEOUT);
							continuation.suspend(response);
							request.setAttribute(CONTINUATION_KEY, continuation);
							startSend(response);
						}
					} else {
						response.sendError(HttpServletResponse.SC_NOT_FOUND);
					}
				}
			} else if ("POST".equals(request.getMethod())) {
				if (is_open) {
					int size = request.getContentLength();
					BufferedReader reader = request.getReader();
					if (size == 0) {
						response.sendError(HttpServletResponse.SC_BAD_REQUEST);
					} else {
						String data = IO.toString(reader);
						if (data.substring(0, 5).equals("data=")) {
							List<SocketIOMessage> list = SocketIOMessage.parse(URIUtil.decodePath(data.substring(5)));
							for (SocketIOMessage msg: list) {
								session.onMessage(msg);
							}
						}
					}
				}
			} else {
	    		response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			}
			
		}

		@Override
		public void onComplete(Continuation cont) {
			if (continuation != null && cont == continuation) {
				if (isConnectionPersistant) {
					is_open = false;
					session.onDisconnect(DisconnectReason.NORMAL);
					continuation = null;
					session.onShutdown();
				} else {
					continuation = null;
					if (!is_open && buffer.isEmpty()) {
						session.onDisconnect(DisconnectReason.NORMAL);
						abort();
					}
					session.startTimeoutTimer();
				}
			}
		}

		@Override
		public void onTimeout(Continuation cont) {
			if (continuation != null && cont == continuation) {
				if (isConnectionPersistant) {
					is_open = false;
					session.onDisconnect(DisconnectReason.TIMEOUT);
					continuation = null;
					session.onShutdown();
				} else {
					continuation = null;
					if (!is_open && buffer.isEmpty()) {
						session.onDisconnect(DisconnectReason.NORMAL);
						abort();
					} else {
						try {
							finishSend(cont.getServletResponse());
						} catch (IOException e) {
							continuation = null;
							session.onDisconnect(DisconnectReason.NORMAL);
							abort();
						}
					}
					session.startTimeoutTimer();
				}
			}
		}

		protected abstract void customConnect(HttpServletRequest request,
				HttpServletResponse response) throws IOException;
		
		public void connect(HttpServletRequest request,
				HttpServletResponse response) throws IOException {
			request.setAttribute(SESSION_KEY, session);
			continuation = ContinuationSupport.getContinuation(request);
			continuation.addContinuationListener(this);
			if (isConnectionPersistant) {
				continuation.setTimeout(HTTP_REQUEST_TIMEOUT*2);
			}
			customConnect(request, response);
			is_open = true;
			session.onConnect(this);
			finishSend(response);
			if (isConnectionPersistant && continuation != null) {
				request.setAttribute(CONTINUATION_KEY, continuation);
				continuation.suspend(response);
			}
		}

		@Override
		public void abort() {
			session.clearHeartbeatTimer();
			session.clearTimeoutTimer();
			is_open = false;
			if (continuation != null) {
				if (continuation.isSuspended()) {
					continuation.complete();
				}
				continuation = null;
				session.onShutdown();
			}
			buffer.setListener(new TransportBuffer.BufferListener() {
				@Override
				public boolean onMessages(List<String> messages) {
					return false;
				}
				
				@Override
				public boolean onMessage(String message) {
					return false;
				}
			});
			buffer.clear();
		}
	}

	public XHRTransport(int bufferSize, int maxIdleTime) {
		this.bufferSize = bufferSize;
		this.maxIdleTime = maxIdleTime;
	}

	protected abstract XHRSessionHelper createHelper(SocketIOSession session);
	
	@Override
	protected SocketIOSession connect(HttpServletRequest request,
			HttpServletResponse response, Factory inboundFactory,
			com.glines.socketio.server.SocketIOSession.Factory sessionFactory)
			throws IOException {
		SocketIOInbound inbound = inboundFactory.getInbound(request, "");
		if (inbound != null) {
 			SocketIOSession session = sessionFactory.createSession(inbound);
			XHRSessionHelper handler =  createHelper(session);
			handler.connect(request, response);
			return session;
		}
		return null;
	}

}
