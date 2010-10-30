package com.glines.socketio.common;

public enum ConnectionState {
	UNKNOWN(-1),
	CONNECTING(0),
	CONNECTED(1),
	CLOSING(2),
	CLOSED(3);

	private int value;
	private ConnectionState(int v) { this.value = v; }
	public int value() { return value; }
	
	public static ConnectionState fromInt(int val) {
		switch (val) {
		case 1:
			return CONNECTING;
		case 2:
			return CONNECTED;
		case 3:
			return CLOSING;
		case 4:
			return CLOSED;
		default:
			return UNKNOWN;
		}
	}
}
