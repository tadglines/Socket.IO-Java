package com.glines.socketio.transport;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import com.glines.socketio.Transport;

public abstract class AbstractTransport implements Transport {
	private static final String MESSAGE_MARKER = "~m~";

	protected String encode(String message) {
		return MESSAGE_MARKER + message.length() + MESSAGE_MARKER + message;
	}

	protected String encode(List<String> messages) {
		StringBuilder str = new StringBuilder();
		for (String msg: messages) {
			str.append(encode(msg));
		}
		return str.toString();
	}
	
	protected String extractSessionId(HttpServletRequest request) {
    	String path = request.getPathInfo();
    	if (path != null && path.length() > 0 && !"/".equals(path)) {
	    	if (path.startsWith("/")) path = path.substring(1);
	    	String[] parts = path.split("/");
	    	if (parts.length >= 2) {
	    		return parts[1] == null ? null : (parts[1].length() == 0 ? null : parts[1]);
	    	}
    	}
    	return null;
	}
	
	protected List<String> decode(String data) {
		ArrayList<String> messages = new ArrayList<String>();
		int idx = 0;

		String str = data.substring(idx, idx+MESSAGE_MARKER.length());
		
		while (data.length() > idx && MESSAGE_MARKER.equals(data.substring(idx, idx+MESSAGE_MARKER.length()))) {
			int start = idx + MESSAGE_MARKER.length();
			int end = start;

			while(data.length() > end && Character.isDigit(data.charAt(end))) {
				end++;
			}

			if (end - start == 0) {
				break;
			}
			
			int size = Integer.parseInt(data.substring(start, end));

			if (data.length() < (end + size)) {
				break;
			}

			start = end + MESSAGE_MARKER.length();
			end = start + size;
			messages.add(data.substring(start, end));
			idx = end;
		}

		return messages;
	}

	@Override
	public void init() {
		
	}

	@Override
	public void destroy() {
		
	}
}
