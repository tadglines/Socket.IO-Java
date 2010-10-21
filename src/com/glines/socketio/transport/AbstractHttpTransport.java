package com.glines.socketio.transport;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.glines.socketio.SocketIOInbound;
import com.glines.socketio.SocketIOSession;
import com.glines.socketio.SocketIOSession.SessionTransportHandler;

public abstract class AbstractHttpTransport extends AbstractTransport {

	public AbstractHttpTransport() {
	}

	protected abstract SocketIOSession connect(
			HttpServletRequest request,
			HttpServletResponse response,
			SocketIOInbound.Factory inboundFactory,
			SocketIOSession.Factory sessionFactory) throws IOException;

	@Override
	public void handle(HttpServletRequest request,
			HttpServletResponse response,
			SocketIOInbound.Factory inboundFactory,
			SocketIOSession.Factory sessionFactory)
			throws IOException {

		String sessionId = extractSessionId(request);

		if ("GET".equals(request.getMethod()) && sessionId == null) {
 			SocketIOSession session = connect(request, response, inboundFactory, sessionFactory);
 			if (session == null) {
 				response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
 			}
		} else {
			SocketIOSession session = sessionFactory.getSession(sessionId);
			if (session != null) {
				SessionTransportHandler handler = session.getTransportHandler();
				if (handler != null) {
					handler.handle(request, response, session);
				} else {
					session.onDisconnect(false);
		    		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				}
			} else {
	    		response.sendError(HttpServletResponse.SC_FORBIDDEN);
			}
		}
	}
}
