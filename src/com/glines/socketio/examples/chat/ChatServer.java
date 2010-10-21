package com.glines.socketio.examples.chat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class ChatServer {
	private static class StaticServlet extends HttpServlet {
		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp)
				throws ServletException, IOException {
			String path = req.getRequestURI();
			path = path.substring(req.getContextPath().length());
			if ("/json.js".equals(path)) {
				resp.setContentType("text/javascript");
				InputStream is = this.getClass().getClassLoader().getResourceAsStream("com/glines/socketio/examples/chat/json.js");
				OutputStream os = resp.getOutputStream();
				byte[] data = new byte[8192];
				int nread = 0;
				while ((nread = is.read(data)) > 0) {
					os.write(data, 0, nread);
					if (nread < data.length) {
						break;
					}
				}
			} else if ("/chat.html".equals(path)) {
				resp.setContentType("text/html");
				InputStream is = this.getClass().getClassLoader().getResourceAsStream("com/glines/socketio/examples/chat/chat.html");
				OutputStream os = resp.getOutputStream();
				byte[] data = new byte[8192];
				int nread = 0;
				while ((nread = is.read(data)) > 0) {
					os.write(data, 0, nread);
					if (nread < data.length) {
						break;
					}
				}
			} else {
				resp.sendRedirect("/chat.html");
			}
		}
	}

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
	    context.addServlet(new ServletHolder(new ChatSocketServlet()), "/socket.io/*");
	    context.addServlet(new ServletHolder(new StaticServlet()), "/*");

	    server.setHandler(context);
	    server.start();
	}

}
