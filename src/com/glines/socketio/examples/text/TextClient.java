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

package com.glines.socketio.examples.text;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.glines.socketio.client.common.SocketIOConnection;
import com.glines.socketio.client.jre.SocketIOConnectionXHRBase;
import com.glines.socketio.common.DisconnectReason;

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
					public void onDisconnect(DisconnectReason reason,
							String errorMessage) {
						print("Disconnected: " + reason + ": " + errorMessage);
						System.exit(-1);
					}

					@Override
					public void onMessage(int messageType, String message) {
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
