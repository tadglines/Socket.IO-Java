package com.glines.socketio.common;

public enum ConnectionState {
	CONNECTING(0),
	CONNECTED(1),
	CLOSING(2),
	CLOSED(3);

	private int value;
	private ConnectionState(int v) { this.value = v; }
	public int value() { return value; }
}