package com.glines.socketio.client.jre;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jetty.client.Address;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.ByteArrayBuffer;
import org.eclipse.jetty.util.URIUtil;

import com.glines.socketio.client.common.SocketIOConnection;
import com.glines.socketio.common.ConnectionState;
import com.glines.socketio.common.DisconnectReason;
import com.glines.socketio.common.SocketIOException;
import com.glines.socketio.server.SocketIOFrame;

public class SocketIOConnectionXHRBase implements SocketIOConnection {
	private final SocketIOConnection.SocketIOConnectionListener listener;
	private final String host;
	private final short port;
	private final String transport;
	private HttpClient client;
	protected String sessionId;
	protected long timeout;
	protected boolean timedout = false;
	protected ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
	protected Future<?> timeoutTask;
	protected ConnectionState state = ConnectionState.CLOSED;
	protected HttpExchange currentGet;
	protected HttpExchange currentPost;
	protected final boolean isConnectionPersistent;
	protected LinkedList<SocketIOFrame> queue = new LinkedList<SocketIOFrame>();
	protected AtomicBoolean doingSend = new AtomicBoolean(false);
	protected AtomicLong messageId = new AtomicLong(0);
	protected String closeId = null;
	protected boolean disconnectWhenEmpty = false;

	public SocketIOConnectionXHRBase(SocketIOConnection.SocketIOConnectionListener listener, 
			String host, short port, String transport, boolean isConnectionPersistent) {
		this.listener = listener;
		this.host = host;
		this.port = port;
		this.transport = transport;
		this.isConnectionPersistent = isConnectionPersistent;
	}
	
	protected void startTimeoutTimer() {
		clearTimeoutTimer();
		if (!timedout && timeout > 0) {
			timeoutTask = executor.schedule(new Runnable() {
				@Override
				public void run() {
					SocketIOConnectionXHRBase.this.onTimeout("Inactivity Timer Timeout");
				}
			}, timeout, TimeUnit.MILLISECONDS);
		}
	}

	protected void clearTimeoutTimer() {
		if (timeoutTask != null) {
			timeoutTask.cancel(false);
			timeoutTask = null;
		}
	}
	
	@Override
	public void connect() {
		if (state != ConnectionState.CLOSED) {
			throw new IllegalStateException("Not CLOSED!");
		}
		state = ConnectionState.CONNECTING;
		client = new HttpClient();
		client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
		client.setIdleTimeout(30*1000); //30 seconds
		try {
			client.start();
		} catch (Exception e) {
			client = null;
			_disconnect(DisconnectReason.CONNECT_FAILED, "Failed to initialize");
			return;
		}
		doGet();
	}

	@Override
	public void close() {
		state = ConnectionState.CLOSING;
		closeId = "" + messageId.get();
		sendFrame(new SocketIOFrame(SocketIOFrame.FrameType.CLOSE, 0, closeId));
	}
	
	@Override
	public void disconnect() {
		_disconnect(DisconnectReason.DISCONNECT, null);
	}

	protected void _disconnect(DisconnectReason reason, String error) {
		switch (state) {
		case CONNECTING:
			listener.onDisconnect(DisconnectReason.CONNECT_FAILED, error);
			break;
		case CLOSED:
			listener.onDisconnect(DisconnectReason.CLOSED, error);
			break;
		case CLOSING:
			if (closeId != null) {
				listener.onDisconnect(DisconnectReason.CLOSE_FAILED, error);
			} else {
				listener.onDisconnect(DisconnectReason.CLOSED_REMOTELY, error);
			}
			break;
		case CONNECTED:
			listener.onDisconnect(reason, error);
			break;
		}
		state = ConnectionState.CLOSED;
		if (currentGet != null) {
			currentGet.cancel();
			currentGet = null;
		}
		if (currentPost != null) {
			currentPost.cancel();
			currentPost = null;
		}
		if (client != null) {
			try {
				client.stop();
			} catch (Exception e) {
				// Ignore
			}
			client = null;
		}
	}
	
	@Override
	public ConnectionState getConnectionState() {
		return state;
	}

	@Override
	public void sendMessage(String message) throws SocketIOException {
		sendMessage(SocketIOFrame.TEXT_MESSAGE_TYPE, message);
	}

	@Override
	public void sendMessage(int messageType, String message) throws SocketIOException {
		sendFrame(new SocketIOFrame(SocketIOFrame.FrameType.DATA, messageType, message));
	}

	protected void sendFrame(SocketIOFrame frame) {
		messageId.incrementAndGet();
		synchronized (queue) {
			queue.add(frame);
		}
		checkSend();
	}
	
	protected void checkSend() {
		if (doingSend.compareAndSet(false, true)) {
			StringBuilder str = new StringBuilder();
			synchronized(queue) {
				for (SocketIOFrame frame: queue) {
					str.append(frame.encode());
				}
				queue.clear();
			}
			doSend(str.toString());
		}
	}
	
	protected void onTimeout(String error) {
		_disconnect(DisconnectReason.TIMEOUT, error);
	}

	protected void onData(String data) {
		startTimeoutTimer();
		onMessageBlock(data);
	}

	protected void onMessageBlock(String data) {
		for (SocketIOFrame frame: SocketIOFrame.parse(data)) {
			onFrame(frame);
		}
	}
	
	protected void onFrame(SocketIOFrame frame) {
		switch (frame.getFrameType()) {
		case SESSION_ID:
			if (state == ConnectionState.CONNECTING) {
				sessionId = frame.getData();
				state = ConnectionState.CONNECTED;
				listener.onConnect();
			}
			break;
		case HEARTBEAT_INTERVAL:
			timeout = Integer.parseInt(frame.getData());
			startTimeoutTimer();
			break;
		case CLOSE:
			onClose(frame.getData());
			break;
		case PING:
			onPing(frame.getData());
			break;
		case PONG:
			onPong(frame.getData());
			break;
		case DATA:
			onMessageData(frame.getMessageType(), frame.getData());
			break;
		default:
			break;
		}
	}

	protected void onClose(String data) {
		if (state == ConnectionState.CLOSING) {
			if (closeId != null && closeId.equals(data)) {
				state = ConnectionState.CLOSED;
				_disconnect(DisconnectReason.DISCONNECT, null);
			}
		} else {
			state = ConnectionState.CLOSING;
			disconnectWhenEmpty = true;
			sendFrame(new SocketIOFrame(SocketIOFrame.FrameType.CLOSE, 0, data));
		}
	}

	protected void onPing(String data) {
		sendFrame(new SocketIOFrame(SocketIOFrame.FrameType.PONG, 0, data));
	}

	protected void onPong(String data) {
		// Ignore
	}

	protected void onMessageData(int messageType, String data) {
		if (messageType == SocketIOFrame.TEXT_MESSAGE_TYPE) {
			listener.onMessage(messageType, data);
		}
	}
	
	protected String getRequestURI() {
		if (sessionId != null) {
			return "/socket.io/" + transport + "/" + sessionId + "/" + System.currentTimeMillis();
		} else {
			return "/socket.io/" + transport + "//" + System.currentTimeMillis();
		}
	}

	protected String getPostURI() {
		return getRequestURI() + "/send";
	}
	
	protected void doGet() {
		HttpExchange exch = new HttpExchange() {
			@Override
			protected void onConnectionFailed(Throwable x) {
				currentGet = null;
				_disconnect(DisconnectReason.ERROR, x.toString());
			}

			@Override
			protected void onException(Throwable x) {
				currentGet = null;
				_disconnect(DisconnectReason.ERROR, x.toString());
			}

			@Override
			protected void onExpire() {
				currentGet = null;
				_disconnect(DisconnectReason.TIMEOUT, "Request timed-out");
			}

			@Override
			protected void onResponseComplete() throws IOException {
				currentGet = null;
				if (!isConnectionPersistent) {
					if (this.getStatus() == 200) {
						if (state != ConnectionState.CLOSED) {
							doGet();
						}
					} else {
						_disconnect(DisconnectReason.ERROR, "HTTP response code: " + this.getStatus());
					}
				}
			}

			@Override
			protected void onResponseContent(Buffer content) throws IOException {
				onData(content.toString());
			}
			
		};
		exch.setMethod("GET");
		exch.setAddress(new Address(host, port));
		exch.setURI(getRequestURI());
		try {
			client.send(exch);
			currentGet = exch;
		} catch (IOException e) {
			_disconnect(DisconnectReason.ERROR, e.toString());
		}
	}

	protected String getPostContentType() {
		return "application/x-www-form-urlencoded; charset=utf-8";
	}

	protected Buffer formatPostData(String data) {
		return new ByteArrayBuffer("data=" + URIUtil.encodePath(data));
	}
	
	protected void doSend(String data) {
		HttpExchange exch = new HttpExchange() {

			@Override
			protected void onConnectionFailed(Throwable x) {
				currentPost = null;
				doingSend.set(false);
				_disconnect(DisconnectReason.ERROR, x.toString());
			}

			@Override
			protected void onException(Throwable x) {
				currentPost = null;
				doingSend.set(false);
				_disconnect(DisconnectReason.ERROR, x.toString());
			}

			@Override
			protected void onExpire() {
				currentPost = null;
				doingSend.set(false);
				_disconnect(DisconnectReason.TIMEOUT, "Request timed-out");
			}

			@Override
			protected void onResponseComplete() throws IOException {
				currentPost = null;
				if (this.getStatus() != 200) {
					_disconnect(DisconnectReason.ERROR, "POST Failed with response code: " + this.getStatus());
				} else {
					doingSend.set(false);
					checkSend();
				}
			}
			
		};
		exch.setMethod("GET");
		exch.setRequestContentType(getPostContentType());
		exch.setRequestContent(formatPostData(data));
		try {
			client.send(exch);
			currentPost = exch;
		} catch (IOException e) {
			doingSend.set(false);
			_disconnect(DisconnectReason.ERROR, "Failed to create SEND request");
		}
	}
}
