package com.glines.socketio.common;

public enum CloseType {
	CLOSE_SIMPLE(1),
	CLOSE_CLEAN(2),
	CLOSE_POLITE(3);

	private int value;
	private CloseType(int v) { this.value = v; }
	public int value() { return value; }
}