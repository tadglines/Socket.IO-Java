package com.glines.socketio.common;

public interface SocketIOMessageParser {
	Object decode(String text) throws SocketIOMessageParserException;
	String encode(Object obj) throws SocketIOMessageParserException;
}
