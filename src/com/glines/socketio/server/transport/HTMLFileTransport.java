package com.glines.socketio.server.transport;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.glines.socketio.server.SocketIOFrame;
import com.glines.socketio.server.SocketIOSession;

public class HTMLFileTransport extends XHRTransport {
	public static final String TRANSPORT_NAME = "htmlfile";

	private class SessionHelper extends XHRSessionHelper {
		SessionHelper(SocketIOSession session) {
			super(session, true);
		}

		protected void startSend(HttpServletResponse response) throws IOException {
			response.setContentType("text/html");
			response.setHeader("Connection", "keep-alive");
			response.setHeader("Transfer-Encoding", "chunked");
			char[] spaces = new char[244];
			Arrays.fill(spaces, ' ');
			ServletOutputStream os = response.getOutputStream();
			os.print("<html><body>" + new String(spaces));
			response.flushBuffer();
		}
		
		protected void writeData(ServletResponse response, String data) throws IOException {
			response.getOutputStream().print("<script>parent.s._("+ data +", document);</script>");
			response.flushBuffer();
		}

		protected void finishSend(ServletResponse response) throws IOException {};

		protected void customConnect(HttpServletRequest request,
				HttpServletResponse response) throws IOException {
			startSend(response);
			writeData(response, SocketIOFrame.encode(SocketIOFrame.Type.SESSION_ID, session.getSessionId()));
			writeData(response, SocketIOFrame.encode(SocketIOFrame.Type.HEARTBEAT_INTERVAL, "" + HEARTBEAT_DELAY));
		}
	}

	public HTMLFileTransport(int bufferSize, int maxIdleTime) {
		super(bufferSize, maxIdleTime);
	}

	@Override
	public String getName() {
		return TRANSPORT_NAME;
	}

	protected XHRSessionHelper createHelper(SocketIOSession session) {
		return new SessionHelper(session);
	}
}
