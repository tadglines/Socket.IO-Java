package com.glines.socketio.server.transport;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.glines.socketio.server.SocketIOFrame;
import com.glines.socketio.server.SocketIOSession;

public class XHRMultipartTransport extends XHRTransport {
	public static final String TRANSPORT_NAME = "xhr-multipart";

	private class XHRMultipartSessionHelper extends XHRSessionHelper {
		XHRMultipartSessionHelper(SocketIOSession session) {
			super(session, true);
		}

		protected void startSend(HttpServletResponse response) throws IOException {
			response.setContentType("multipart/x-mixed-replace;boundary=\"socketio\"");
			response.setHeader("Connection", "keep-alive");
			char[] spaces = new char[244];
			Arrays.fill(spaces, ' ');
			ServletOutputStream os = response.getOutputStream();
			os.print("--socketio");
			response.flushBuffer();
		}

		protected void writeData(ServletResponse response, String data) throws IOException {
			System.out.println("Session["+session.getSessionId()+"]: writeData(START): " + data);
			ServletOutputStream os = response.getOutputStream();
			os.println("Content-Type: text/plain");
			os.println();
			os.println(data);
			os.println("--socketio");
			response.flushBuffer();
			System.out.println("Session["+session.getSessionId()+"]: writeData(END): " + data);
		}

		protected void finishSend(ServletResponse response) throws IOException {};

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
		return new XHRMultipartSessionHelper(session);
	}
}
