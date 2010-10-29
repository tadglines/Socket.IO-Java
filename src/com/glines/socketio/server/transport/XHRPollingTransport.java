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

import java.io.IOException;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.glines.socketio.common.CloseType;
import com.glines.socketio.common.ConnectionState;
import com.glines.socketio.common.SocketIOException;
import com.glines.socketio.server.SocketIOFrame;
import com.glines.socketio.server.SocketIOSession;

public class XHRPollingTransport extends XHRTransport {
	public static final String TRANSPORT_NAME = "xhr-polling";

	protected class XHRPollingSessionHelper extends XHRSessionHelper {

		XHRPollingSessionHelper(SocketIOSession session) {
			super(session, false);
		}

		protected void startSend(HttpServletResponse response) throws IOException {
			response.setContentType("text/plain; charset=UTF-8");
		}

		@Override
		protected void writeData(ServletResponse response, String data) throws IOException {
			response.getOutputStream().print(data);
			response.flushBuffer();
		}

		protected void finishSend(ServletResponse response) throws IOException {};

		protected void customConnect(HttpServletRequest request,
				HttpServletResponse response) throws IOException {
			startSend(response);
			writeData(response, SocketIOFrame.encode(SocketIOFrame.Type.SESSION_ID, session.getSessionId()));
			writeData(response, SocketIOFrame.encode(SocketIOFrame.Type.HEARTBEAT_INTERVAL, "" + REQUEST_TIMEOUT));
		}
	}
	
	public XHRPollingTransport(int bufferSize, int maxIdleTime) {
		super(bufferSize, maxIdleTime);
	}

	@Override
	public String getName() {
		return TRANSPORT_NAME;
	}
	

	protected XHRPollingSessionHelper createHelper(SocketIOSession session) {
		return new XHRPollingSessionHelper(session);
	}
}
