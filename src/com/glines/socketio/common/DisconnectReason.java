package com.glines.socketio.common;

public enum DisconnectReason {
	CONNECT_FAILED(1),
	DISCONNECT(2),
	TIMEOUT(3),
	CLOSE_FAILED(4),
	ERROR(5);

	private int value;
	private DisconnectReason(int v) { this.value = v; }
	public int value() { return value; }
}