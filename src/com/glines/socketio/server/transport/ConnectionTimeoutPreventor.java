package com.glines.socketio.server.transport;

import org.eclipse.jetty.io.AsyncEndPoint;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.nio.SelectChannelEndPoint;
import org.eclipse.jetty.server.HttpConnection;

/**
 * Jetty will close a connection even if there is continuous outbound data if the request's response
 * is not completed within maxIdleTime milliseconds. This is not appropriate for a persistent connection
 * SocketIO transport. IN order to prevent this, without disabling the maxIdleTime completely,
 * this class is used to obtain a @{link IdleCheck} instance that can be used to reset the idle timeout.
 */
public class ConnectionTimeoutPreventor {
	interface IdleCheck {
		void activity();
	}

	/**
	 * This must be called within the context of an active HTTP request.
	 */
	public static IdleCheck newTimeoutPreventor() {
		HttpConnection httpConnection = HttpConnection.getCurrentConnection();
		if (httpConnection != null) {
			EndPoint endPoint = httpConnection.getEndPoint();
			if (endPoint instanceof AsyncEndPoint) {
				((AsyncEndPoint)endPoint).cancelIdle();
			}
			if (endPoint instanceof SelectChannelEndPoint) {
				final SelectChannelEndPoint scep = (SelectChannelEndPoint)endPoint;
				scep.cancelIdle();
				return new IdleCheck() {
					@Override
					public void activity() {
						scep.scheduleIdle();
					}
				};
			} else {
				return new IdleCheck() {
					@Override
					public void activity() {
						// Do nothing
					}
				};
			}
		} else {
			return null;
		}
	}
}
