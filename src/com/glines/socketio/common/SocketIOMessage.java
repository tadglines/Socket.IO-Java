package com.glines.socketio.common;

import java.util.ArrayList;
import java.util.List;

public class SocketIOMessage {
	public enum Type {
		UNKNOWN(-1),
		SESSION_ID(1),
		CLOSE(2),
		PING(3),
		PONG(4),
		TEXT(5),
		JSON(6);

		private int value;
		
		Type(int value) {
			this.value = value;
		}
		
		public int value() {
			return value;
		}
		
		public static Type fromInt(int val) {
			switch (val) {
			case 1:
				return SESSION_ID;
			case 2:
				return CLOSE;
			case 3:
				return PING;
			case 4:
				return PONG;
			case 5:
				return TEXT;
			case 6:
				return JSON;
			default:
				return UNKNOWN;
			}
		}
	}

	private static boolean isNumber(String str, int start, int end) {
		for (int i = start; i < end; i++) {
			if (!Character.isDigit(str.charAt(i))) {
				return false;
			}
		}
		return true;
	}
	
	public static List<SocketIOMessage> parse(String data) {
		List<SocketIOMessage> messages = new ArrayList<SocketIOMessage>();
		int idx = 0;

		// Parse the data and silently ignore any part that fails to parse properly.
		while (data.length() > idx && data.charAt(idx) == ':') {
			int start = idx + 1;
			int end = data.indexOf(':', start);

			if (-1 == end || start == end || !isNumber(data, start, end)) {
				break;
			}
			
			int type = Integer.parseInt(data.substring(start, end));

			start = end + 1;
			end = data.indexOf(':', start);

			if (-1 == end || start == end || !isNumber(data, start, end)) {
				break;
			}
			
			int size = Integer.parseInt(data.substring(start, end));

			start = end + 1;
			end = start + size;

			if (data.length() < end) {
				break;
			}

			messages.add(new SocketIOMessage(Type.fromInt(type), data.substring(start, end)));
			idx = end;
		}
		
		return messages;
	}
	
	public static String encode(Type type, String data) {
		StringBuilder str = new StringBuilder(data.length() + 16);
		str.append(':');
		str.append(type.value());
		str.append(':');
		str.append(data.length());
		str.append(':');
		str.append(data);
		return str.toString();
	}
	
	private final Type type;
	private final String data;
	
	public SocketIOMessage(Type type, String data) {
		this.type = type;
		this.data = data;
	}
	
	public Type getType() {
		return type;
	}
	
	public String getData() {
		return data;
	}
	
	public String encode() {
		return encode(type, data);
	}
}
