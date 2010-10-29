package com.glines.socketio.server;

import java.util.ArrayList;
import java.util.List;

public class SocketIOFrame {
	public static final char SEPERATOR_CHAR = '~';
	public enum Type {
		UNKNOWN(-1),
		SESSION_ID(1),
		HEARTBEAT_INTERVAL(2),
		CLOSE(3),
		PING(4),
		PONG(5),
		TEXT(6);

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
				return HEARTBEAT_INTERVAL;
			case 3:
				return CLOSE;
			case 4:
				return PING;
			case 5:
				return PONG;
			case 6:
				return TEXT;
			default:
				return UNKNOWN;
			}
		}
	}

	public static final int TEXT_MESSAGE_TYPE = 0;
	public static final int JSON_MESSAGE_TYPE = 1;
	
	private static boolean isNumber(String str, int start, int end) {
		for (int i = start; i < end; i++) {
			if (!Character.isDigit(str.charAt(i))) {
				return false;
			}
		}
		return true;
	}
	
	public static List<SocketIOFrame> parse(String data) {
		List<SocketIOFrame> messages = new ArrayList<SocketIOFrame>();
		int idx = 0;

		// Parse the data and silently ignore any part that fails to parse properly.
		while (data.length() > idx && data.charAt(idx) == SEPERATOR_CHAR) {
			int start = idx + 1;
			int end = data.indexOf(SEPERATOR_CHAR, start);

			if (-1 == end || start == end || !isNumber(data, start, end)) {
				break;
			}
			
			int type = Integer.parseInt(data.substring(start, end));

			start = end + 1;
			end = data.indexOf(SEPERATOR_CHAR, start);

			if (-1 == end || start == end || !isNumber(data, start, end)) {
				break;
			}
			
			int size = Integer.parseInt(data.substring(start, end));

			start = end + 1;
			end = start + size;

			if (data.length() < end) {
				break;
			}

			messages.add(new SocketIOFrame(Type.fromInt(type), data.substring(start, end)));
			idx = end;
		}
		
		return messages;
	}
	
	public static String encode(Type type, String data) {
		StringBuilder str = new StringBuilder(data.length() + 16);
		str.append(SEPERATOR_CHAR);
		str.append(type.value());
		str.append(SEPERATOR_CHAR);
		str.append(data.length());
		str.append(SEPERATOR_CHAR);
		str.append(data);
		return str.toString();
	}
	
	private final Type type;
	private final String data;
	
	public SocketIOFrame(Type type, String data) {
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
