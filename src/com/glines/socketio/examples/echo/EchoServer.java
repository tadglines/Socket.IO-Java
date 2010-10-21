package com.glines.socketio.examples.echo;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

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
	    context.addServlet(new ServletHolder(new EchoSocketServlet()), "/socket.io/*");

	    server.setHandler(context);
	    server.start();
	}

}
