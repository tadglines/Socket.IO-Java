package com.glines.socketio.server.transport;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.glines.socketio.server.SocketIOFrame;
import com.glines.socketio.server.SocketIOSession;
import com.glines.socketio.server.transport.ConnectionTimeoutPreventor.IdleCheck;

public class XHRMultipartTransport extends XHRTransport {
	public static final String TRANSPORT_NAME = "xhr-multipart";
	private static final int MULTIPART_BOUNDARY_LENGTH = 20;

	private class XHRMultipartSessionHelper extends XHRSessionHelper {
		private final String contentType;
		private final String boundary;
		private final String boundarySeperator;
		private final IdleCheck idleCheck;
		
		XHRMultipartSessionHelper(SocketIOSession session, IdleCheck idleCheck) {
			super(session, true);
			boundary = session.generateRandomString(MULTIPART_BOUNDARY_LENGTH);
			boundarySeperator = "--" + boundary;
			contentType = "multipart/x-mixed-replace;boundary=\""+boundary+"\"";
			this.idleCheck = idleCheck;
		}

		protected void startSend(HttpServletResponse response) throws IOException {
			response.setContentType(contentType);
			response.setHeader("Connection", "keep-alive");
			char[] spaces = new char[244];
			Arrays.fill(spaces, ' ');
			ServletOutputStream os = response.getOutputStream();
			os.print(boundarySeperator);
			response.flushBuffer();
		}

		protected void writeData(ServletResponse response, String data) throws IOException {
			idleCheck.activity();
			System.out.println("Session["+session.getSessionId()+"]: writeData(START): " + data);
			ServletOutputStream os = response.getOutputStream();
			os.println("Content-Type: text/plain");
			os.println();
			os.println(data);
			os.println(boundarySeperator);
			response.flushBuffer();
			System.out.println("Session["+session.getSessionId()+"]: writeData(END): " + data);
		}

		protected void finishSend(ServletResponse response) throws IOException {
		};

		protected void customConnect(HttpServletRequest request,
				HttpServletResponse response) throws IOException {
			startSend(response);
			writeData(response, SocketIOFrame.encode(SocketIOFrame.FrameType.SESSION_ID, 0, session.getSessionId()));
			writeData(response, SocketIOFrame.encode(SocketIOFrame.FrameType.HEARTBEAT_INTERVAL, 0, "" + HEARTBEAT_DELAY));
		}
	}

	public XHRMultipartTransport(int bufferSize, int maxIdleTime) {
		super(bufferSize, maxIdleTime);
	}

	@Override
	public String getName() {
		return TRANSPORT_NAME;
	}

	protected XHRSessionHelper createHelper(SocketIOSession session) {
		IdleCheck idleCheck = ConnectionTimeoutPreventor.newTimeoutPreventor();
		return new XHRMultipartSessionHelper(session, idleCheck);
	}
}
