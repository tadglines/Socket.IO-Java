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

package com.glines.socketio.server.transport;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.glines.socketio.server.Transport;

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
