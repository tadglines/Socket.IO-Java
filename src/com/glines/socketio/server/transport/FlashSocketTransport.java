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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.glines.socketio.server.SocketIOInbound;
import com.glines.socketio.server.SocketIOSession;

public class FlashSocketTransport extends WebSocketTransport {
	public static final String TRANSPORT_NAME = "flashsocket";
	private static final String FLASHFILE_PATH = TRANSPORT_NAME + "/lib/vendor/web-socket-js/WebSocketMain.swf";
  	private static final String FLASHFILE_NAME = "WebSocketMain.swf";
	private ServerSocketChannel flashPolicyServer = null;
	private ExecutorService executor = Executors.newCachedThreadPool();
	private Future<?> policyAcceptorThread = null;
	private short httpPort = 8080;
	private String flashPolicyDomain = "localhost";


	public FlashSocketTransport(int bufferSize, int maxIdleTime) {
		super(bufferSize, maxIdleTime);
	}

	@Override
	public String getName() {
		return TRANSPORT_NAME;
	}

	@Override
	public void init() {
		try {
			startFlashPolicyServer();
		} catch (IOException e) {
			// Ignore
		}
	}

	@Override
	public void destroy() {
		stopFlashPolicyServer();
	}

	@Override
	public void handle(HttpServletRequest request,
			HttpServletResponse response,
			SocketIOInbound.Factory inboundFactory,
			SocketIOSession.Factory sessionFactory)
			throws IOException {

		String path = request.getPathInfo();
    	if (path == null || path.length() == 0 || "/".equals(path)) {
    		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid " + TRANSPORT_NAME + " transport request");
    		return;
    	}
    	if (path.startsWith("/")) path = path.substring(1);
    	String[] parts = path.split("/");

    	if ("GET".equals(request.getMethod()) && TRANSPORT_NAME.equals(parts[0])) {
    		if (!FLASHFILE_PATH.equals(path)) {
        		super.handle(request, response, inboundFactory, sessionFactory);
    		} else {
				response.setContentType("application/x-shockwave-flash");
				InputStream is = this.getClass().getClassLoader().getResourceAsStream(FLASHFILE_NAME);
				OutputStream os = response.getOutputStream();
				byte[] data = new byte[8192];
				int nread = 0;
				while ((nread = is.read(data)) > 0) {
					os.write(data, 0, nread);
					if (nread < data.length) {
						break;
					}
				}
    		}
    	} else {
    		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid " + TRANSPORT_NAME + " transport request");
    	}
	}
	
	/**
	 * Starts this server, binding to the previously passed SocketAddress.
	 */
	public void startFlashPolicyServer() throws IOException {
		final String POLICY_FILE_REQUEST = "<policy-file-request/>";
		flashPolicyServer = ServerSocketChannel.open();
		flashPolicyServer.socket().setReuseAddress(true);
		flashPolicyServer.socket().bind(new InetSocketAddress("0.0.0.0", 843));
		flashPolicyServer.configureBlocking(true);

		// Spawn a new server acceptor thread, which must accept incoming
		// connections indefinitely - until a ClosedChannelException is thrown.
		policyAcceptorThread = executor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					while (true) {
						final SocketChannel serverSocket = flashPolicyServer.accept();
						executor.submit(new Runnable() {
							@Override
							public void run() {
								try {
									serverSocket.configureBlocking(true);
									Socket s = serverSocket.socket();
									StringBuilder request = new StringBuilder();
									InputStreamReader in = new InputStreamReader(s.getInputStream());
									int c;
									while ((c = in.read()) != 0 && request.length() <= POLICY_FILE_REQUEST.length()) {
										request.append((char)c);
									}
									if (request.toString().equalsIgnoreCase(POLICY_FILE_REQUEST)) {
										PrintWriter out = new PrintWriter(s.getOutputStream());
										out.println("<cross-domain-policy><allow-access-from domain=\""+flashPolicyDomain+"\" to-ports=\""+httpPort+"\" /></cross-domain-policy>");
										out.write(0);
										out.flush();
									}
									serverSocket.close();
								} catch (IOException e) {
									// TODO: Add loging
								} finally {
									try {
										serverSocket.close();
									} catch (IOException e) {
										// Ignore error on close.
									}
								}
							}
						});
					}
				} catch (ClosedChannelException e) {
					return;
				} catch (IOException e) {
					throw new IllegalStateException("Server should not throw a misunderstood IOException", e);
				}
			}
		});
	}

	private void stopFlashPolicyServer() {
		if (flashPolicyServer != null) {
			try {
				flashPolicyServer.close();
			} catch (IOException e) {
				// Ignore
			}
		}
		if (policyAcceptorThread != null) {
			try {
				policyAcceptorThread.get();
			} catch (InterruptedException e) {
				throw new IllegalStateException();
			} catch (ExecutionException e) {
				throw new IllegalStateException("Server thread threw an exception", e.getCause());
			}
			if (!policyAcceptorThread.isDone()) {
				throw new IllegalStateException("Server acceptor thread has not stopped.");
			}
		}
	}

}
