package com.glines.socketio.server.transport;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.ajax.JSON;

import com.glines.socketio.server.SocketIOFrame;
import com.glines.socketio.server.SocketIOSession;
import com.glines.socketio.server.transport.ConnectionTimeoutPreventor.IdleCheck;

public class HTMLFileTransport extends XHRTransport {
	public static final String TRANSPORT_NAME = "htmlfile";

	private class HTMLFileSessionHelper extends XHRSessionHelper {
		private final IdleCheck idleCheck;

		HTMLFileSessionHelper(SocketIOSession session, IdleCheck idleCheck) {
			super(session, true);
			this.idleCheck = idleCheck;
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
			idleCheck.activity();
			response.getOutputStream().print("<script>parent.s._("+ JSON.toString(data) +", document);</script>");
			response.flushBuffer();
		}

		protected void finishSend(ServletResponse response) throws IOException {};

		protected void customConnect(HttpServletRequest request,
				HttpServletResponse response) throws IOException {
			startSend(response);
			writeData(response, SocketIOFrame.encode(SocketIOFrame.FrameType.SESSION_ID, 0, session.getSessionId()));
			writeData(response, SocketIOFrame.encode(SocketIOFrame.FrameType.HEARTBEAT_INTERVAL, 0, "" + HEARTBEAT_DELAY));
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
		IdleCheck idleCheck = ConnectionTimeoutPreventor.newTimeoutPreventor();
		return new HTMLFileSessionHelper(session, idleCheck);
	}
}
