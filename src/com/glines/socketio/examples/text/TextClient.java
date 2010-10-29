package com.glines.socketio.examples.text;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.glines.socketio.client.common.SocketIOConnection;
import com.glines.socketio.client.jre.SocketIOConnectionXHRBase;
import com.glines.socketio.common.CloseType;
import com.glines.socketio.common.DisconnectReason;
import com.glines.socketio.common.SocketIOException;

public class TextClient {
	public static synchronized void print(String str) {
		System.out.println(str);
		System.out.flush();
	}
	
	public static void main(String[] args) throws Exception {
		String host = "localhost";
		int port = 8080;
		String transport;

		if (args.length < 3) {
			System.exit(-1);
		}

		host = args[0];
		port = Integer.parseInt(args[1]);
		transport = args[2];

		
		SocketIOConnectionXHRBase client = new SocketIOConnectionXHRBase(
				new SocketIOConnection.SocketIOConnectionListener() {

					@Override
					public void onConnect() {
						print("Connected");
					}

					@Override
					public void onClose(CloseType requestedType,
							CloseType result) {
						print("Closed: " + requestedType + ": " + result);
						System.exit(0);
					}

					@Override
					public void onDisconnect(DisconnectReason reason,
							String errorMessage) {
						print("Disconnected: " + reason + ": " + errorMessage);
						System.exit(-1);
					}

					@Override
					public void onMessage(int messageType, Object message,
							SocketIOException parseError) {
						print((String)message);
					}
					
				},
				host, (short)port, transport, false);
		client.connect();
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String line;
		while ((line = in.readLine()) != null) {
			client.sendMessage(line);
		}
	}
}
