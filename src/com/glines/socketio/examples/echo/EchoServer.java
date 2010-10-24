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

package com.glines.socketio.examples.echo;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.glines.socketio.examples.chat.ChatSocketServlet;
import com.glines.socketio.server.transport.FlashSocketTransport;

public class EchoServer {
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		String host = "localhost";
		int port = 8080;

		if (args.length > 0) {
			host = args[0];
		}

		if (args.length > 1) {
			port = Integer.parseInt(args[1]);
		}
		
		Server server = new Server();
	    SelectChannelConnector connector = new SelectChannelConnector();
	    connector.setHost(host);
	    connector.setPort(port);

	    server.addConnector(connector);

	    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
	    ServletHolder holder = new ServletHolder(new ChatSocketServlet());
	    holder.setInitParameter(FlashSocketTransport.FLASHPOLICY_SERVER_HOST_KEY, host);
	    holder.setInitParameter(FlashSocketTransport.FLASHPOLICY_DOMAIN_KEY, host);
	    holder.setInitParameter(FlashSocketTransport.FLASHPOLICY_PORTS_KEY, ""+ port);
	    context.addServlet(holder, "/socket.io/*");

	    server.setHandler(context);
	    server.start();
	}

}
